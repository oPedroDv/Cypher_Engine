package com.cypher.analysis.domain;

import com.cypher.shared.exception.InvalidNFeException;
import com.cypher.shared.util.AccessKeyValidator;
import com.cypher.shared.util.CnpjValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class NFeParser {

    private static final int MAX_XML_BYTES = 1024 * 1024;
    private static final int MAX_BASE64_BYTES = ((MAX_XML_BYTES + 2) / 3) * 4;
    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("0.01");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final boolean requireSignature;
    private final KeyStore trustStore;

    @Autowired
    public NFeParser(
            @Value("${cypher.nfe.require-signature:true}") boolean requireSignature,
            @Value("${cypher.nfe.trust-store.path:}") String trustStorePath,
            @Value("${cypher.nfe.trust-store.password:}") String trustStorePassword
    ) {
        this.requireSignature = requireSignature;
        this.trustStore = requireSignature ? loadTrustStore(trustStorePath, trustStorePassword) : null;
    }

    NFeParser(boolean requireSignature) {
        this.requireSignature = requireSignature;
        this.trustStore = null;
    }

    public NFeData parse(String input) {
        validateInput(input);
        String xml = decodeIfBase64(input);

        if (xml.getBytes(StandardCharsets.UTF_8).length > MAX_XML_BYTES) {
            throw new InvalidNFeException("XML da NF-e excede o tamanho máximo permitido (1MB)");
        }

        try {
            Document document = buildDocument(xml);
            Element infNFe = validateStructure(document);
            String accessKey = normalizeKey(extractAccessKey(document, infNFe));

            String issuerCnpj = requiredNested(document, "emit", "CNPJ", "CNPJ do emitente ausente no XML");
            issuerCnpj = CnpjValidator.strip(issuerCnpj);
            validateIdentifiers(accessKey, issuerCnpj, infNFe);

            String recipientCnpj = extractNested(document, "dest", "CNPJ", null);
            if (recipientCnpj != null) {
                recipientCnpj = CnpjValidator.strip(recipientCnpj);
                if (!CnpjValidator.isValid(recipientCnpj)) {
                    throw new InvalidNFeException("CNPJ do destinatário inválido");
                }
            } else {
                String cpf = extractNested(document, "dest", "CPF", null);
                if (cpf == null || !isValidCpf(cpf)) {
                    throw new InvalidNFeException("CNPJ/CPF do destinatário ausente ou inválido no XML");
                }
                recipientCnpj = cpf.replaceAll("[^0-9]", "");
            }

            String rawIssueDate = extractOptional(document, "dhEmi", extractOptional(document, "dEmi", null));
            LocalDate issueDate = parseDate(rawIssueDate)
                    .orElseThrow(() -> new InvalidNFeException("Data de emissão ausente ou inválida no XML"));

            validateSignature(document, infNFe, issueDate);

            Element totals = requiredElement(document, "ICMSTot", "Grupo total/ICMSTot ausente no XML");
            BigDecimal totalAmount = requiredMoney(totals, "vNF");
            BigDecimal productsAmount = requiredMoney(totals, "vProd");
            BigDecimal freightAmount = optionalMoney(totals, "vFrete");
            BigDecimal discountAmount = optionalMoney(totals, "vDesc");
            validateTotal(totals, totalAmount, productsAmount, freightAmount, discountAmount);

            String rawDueDate = extractOptional(document, "dVenc", null);
            LocalDate dueDate = parseDate(rawDueDate).orElse(issueDate.plusDays(30));

            return NFeData.builder()
                    .accessKey(accessKey)
                    .issuerCnpj(issuerCnpj)
                    .issuerLegalName(extractNested(document, "emit", "xNome", "EMITENTE DESCONHECIDO"))
                    .recipientCnpj(recipientCnpj)
                    .recipientLegalName(extractNested(document, "dest", "xNome", "DESTINATARIO DESCONHECIDO"))
                    .totalAmount(totalAmount)
                    .productsAmount(productsAmount)
                    .freightAmount(freightAmount)
                    .discountAmount(discountAmount)
                    .issueDate(issueDate)
                    .dueDate(dueDate)
                    .status(InvoiceStatus.PENDING)
                    .build();
        } catch (InvalidNFeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidNFeException("Falha ao processar XML da NF-e: " + e.getMessage(), e);
        }
    }

    private Element validateStructure(Document document) {
        Element root = document.getDocumentElement();
        String rootName = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
        if (!"NFe".equals(rootName) && !"nfeProc".equals(rootName)) {
            throw new InvalidNFeException("Documento não possui estrutura NF-e/nfeProc válida");
        }
        Element infNFe = requiredElement(document, "infNFe", "Grupo infNFe ausente no XML");
        if (!"4.00".equals(infNFe.getAttribute("versao"))) {
            throw new InvalidNFeException("Somente NF-e versão 4.00 é aceita");
        }
        return infNFe;
    }

    private void validateIdentifiers(String accessKey, String issuerCnpj, Element infNFe) {
        if (!AccessKeyValidator.isValid(accessKey)) {
            throw new InvalidNFeException("Chave NF-e possui dígito verificador inválido");
        }
        if (!CnpjValidator.isValid(issuerCnpj)) {
            throw new InvalidNFeException("CNPJ do emitente inválido");
        }
        if (!issuerCnpj.equals(AccessKeyValidator.extractIssuerCnpj(accessKey))) {
            throw new InvalidNFeException("CNPJ do emitente diverge do CNPJ contido na chave NF-e");
        }
        if (!("NFe" + accessKey).equals(infNFe.getAttribute("Id"))) {
            throw new InvalidNFeException("Id de infNFe diverge da chave de acesso");
        }
    }

    private void validateSignature(Document document, Element infNFe, LocalDate issueDate) {
        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (signatures.getLength() == 0) {
            if (requireSignature) throw new InvalidNFeException("Assinatura digital da NF-e ausente");
            return;
        }
        if (signatures.getLength() != 1) {
            throw new InvalidNFeException("NF-e deve conter exatamente uma assinatura digital");
        }
        try {
            infNFe.setIdAttribute("Id", true);
            Date signingDate = Date.from(issueDate.atTime(LocalTime.NOON).toInstant(ZoneOffset.UTC));
            DOMValidateContext context =
                    new DOMValidateContext(new NFeKeySelector(trustStore, signingDate), signatures.item(0));
            context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
            XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
            String expectedReference = "#" + infNFe.getAttribute("Id");
            boolean referencesInvoice = signature.getSignedInfo().getReferences().stream()
                    .anyMatch(reference -> expectedReference.equals(reference.getURI()));
            if (!referencesInvoice || !signature.validate(context)) {
                throw new InvalidNFeException("Assinatura digital da NF-e inválida");
            }
        } catch (InvalidNFeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidNFeException("Não foi possível validar a assinatura digital da NF-e", e);
        }
    }

    private void validateTotal(
            Element totals,
            BigDecimal total,
            BigDecimal products,
            BigDecimal freight,
            BigDecimal discount
    ) {
        if (total.signum() <= 0) throw new InvalidNFeException("vNF deve ser maior que zero");
        BigDecimal expected = products
                .add(freight)
                .add(optionalMoney(totals, "vSeg"))
                .add(optionalMoney(totals, "vOutro"))
                .add(optionalMoney(totals, "vST"))
                .add(optionalMoney(totals, "vFCPST"))
                .add(optionalMoney(totals, "vII"))
                .add(optionalMoney(totals, "vIPI"))
                .add(optionalMoney(totals, "vIPIDevol"))
                .subtract(discount)
                .subtract(optionalMoney(totals, "vICMSDeson"));
        if (expected.subtract(total).abs().compareTo(TOTAL_TOLERANCE) > 0) {
            throw new InvalidNFeException("vNF é incompatível com os componentes monetários declarados");
        }
    }

    private BigDecimal requiredMoney(Element parent, String tag) {
        String value = extractOptional(parent, tag, null);
        if (value == null) throw new InvalidNFeException("Campo monetário obrigatório ausente: " + tag);
        return parseMoney(tag, value);
    }

    private BigDecimal optionalMoney(Element parent, String tag) {
        String value = extractOptional(parent, tag, null);
        return value == null ? BigDecimal.ZERO : parseMoney(tag, value);
    }

    private BigDecimal parseMoney(String tag, String value) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() < 0) throw new InvalidNFeException("Campo monetário não pode ser negativo: " + tag);
            return amount;
        } catch (NumberFormatException e) {
            throw new InvalidNFeException("Campo monetário inválido: " + tag, e);
        }
    }

    private String extractAccessKey(Document document, Element infNFe) {
        String protocolKey = extractOptional(document, "chNFe", null);
        if (protocolKey != null) return protocolKey;
        String id = infNFe.getAttribute("Id");
        if (id.startsWith("NFe")) return id.substring(3);
        throw new InvalidNFeException("Chave NF-e ausente no XML");
    }

    private String normalizeKey(String key) {
        if (key == null || !key.matches("\\d{44}")) {
            throw new InvalidNFeException("Chave NF-e inválida: esperado exatamente 44 dígitos");
        }
        return key;
    }

    private String requiredNested(Document document, String parent, String child, String message) {
        String value = extractNested(document, parent, child, null);
        if (value == null) throw new InvalidNFeException(message);
        return value;
    }

    private String extractNested(Document document, String parentTag, String childTag, String defaultValue) {
        NodeList parents = elements(document, parentTag);
        if (parents.getLength() == 0) return defaultValue;
        NodeList children = elements((Element) parents.item(0), childTag);
        if (children.getLength() == 0) return defaultValue;
        String value = children.item(0).getTextContent().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private Optional<LocalDate> parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Optional.empty();
        try {
            return Optional.of(dateStr.contains("T")
                    ? LocalDate.parse(dateStr, ISO_OFFSET_DATE_TIME)
                    : LocalDate.parse(dateStr, ISO_DATE));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void validateInput(String input) {
        if (input == null || input.isBlank()) throw new InvalidNFeException("XML da NF-e não pode ser vazio");
        int bytes = input.getBytes(StandardCharsets.UTF_8).length;
        if (input.stripLeading().startsWith("<")) {
            if (bytes > MAX_XML_BYTES) throw new InvalidNFeException("XML da NF-e excede o tamanho máximo permitido (1MB)");
        } else if (bytes > MAX_BASE64_BYTES) {
            throw new InvalidNFeException("Payload Base64 da NF-e excede o tamanho máximo permitido");
        }
    }

    private String decodeIfBase64(String input) {
        if (input.stripLeading().startsWith("<")) return input;
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            String xml = new String(decoded, StandardCharsets.UTF_8);
            if (!xml.stripLeading().startsWith("<")) throw new InvalidNFeException("Payload não contém XML válido");
            return xml;
        } catch (IllegalArgumentException e) {
            throw new InvalidNFeException("Payload deve ser XML ou Base64 válido", e);
        }
    }

    private Document buildDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Element requiredElement(Document document, String tag, String message) {
        NodeList nodes = elements(document, tag);
        if (nodes.getLength() == 0) throw new InvalidNFeException(message);
        return (Element) nodes.item(0);
    }

    private String extractOptional(Node parent, String tag, String defaultValue) {
        NodeList nodes = elements(parent, tag);
        if (nodes.getLength() == 0) return defaultValue;
        String value = nodes.item(0).getTextContent().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private NodeList elements(Node parent, String tag) {
        if (parent instanceof Document document) return document.getElementsByTagNameNS("*", tag);
        return ((Element) parent).getElementsByTagNameNS("*", tag);
    }

    private boolean isValidCpf(String cpf) {
        String digits = cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) return false;
        for (int position = 9; position <= 10; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) sum += Character.digit(digits.charAt(i), 10) * (position + 1 - i);
            int digit = 11 - (sum % 11);
            if (digit >= 10) digit = 0;
            if (digit != Character.digit(digits.charAt(position), 10)) return false;
        }
        return true;
    }

    private static KeyStore loadTrustStore(String path, String password) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                    "NFE_TRUST_STORE_PATH é obrigatório quando a validação de assinatura NF-e está habilitada");
        }
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            String type = path.toLowerCase().endsWith(".jks") ? "JKS" : "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(input, password == null ? new char[0] : password.toCharArray());
            if (keyStore.size() == 0) throw new IllegalStateException("Trust store NF-e não contém certificados");
            return keyStore;
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível carregar o trust store NF-e", e);
        }
    }

    private static final class NFeKeySelector extends KeySelector {
        private final KeyStore trustStore;
        private final Date signingDate;

        private NFeKeySelector(KeyStore trustStore, Date signingDate) {
            this.trustStore = trustStore;
            this.signingDate = signingDate;
        }

        @Override
        public KeySelectorResult select(
                KeyInfo keyInfo,
                Purpose purpose,
                AlgorithmMethod method,
                XMLCryptoContext context
        ) throws KeySelectorException {
            if (keyInfo == null) throw new KeySelectorException("KeyInfo ausente");
            for (XMLStructure structure : keyInfo.getContent()) {
                PublicKey publicKey = extractPublicKey(structure, trustStore, signingDate);
                if (publicKey != null) return () -> publicKey;
            }
            throw new KeySelectorException("Certificado/chave pública ausente em KeyInfo");
        }

        private static PublicKey extractPublicKey(
                XMLStructure structure,
                KeyStore trustStore,
                Date signingDate
        ) throws KeySelectorException {
            try {
                if (structure instanceof X509Data x509Data) {
                    Collection<X509Certificate> certificates = new ArrayList<>();
                    for (Object item : x509Data.getContent()) {
                        if (item instanceof X509Certificate certificate) {
                            certificate.checkValidity(signingDate);
                            certificates.add(certificate);
                        }
                    }
                    if (!certificates.isEmpty()) {
                        List<X509Certificate> chain = leafFirst(certificates);
                        X509Certificate leaf = chain.get(0);
                        if (leaf.getBasicConstraints() >= 0) {
                            throw new KeySelectorException(
                                    "Certificado de assinatura não pode ser de autoridade certificadora");
                        }
                        boolean[] keyUsage = leaf.getKeyUsage();
                        if (keyUsage != null && (keyUsage.length == 0 || !keyUsage[0])) {
                            throw new KeySelectorException("Certificado não permite assinatura digital");
                        }
                        if (trustStore != null) validateCertificateChain(chain, trustStore, signingDate);
                        return leaf.getPublicKey();
                    }
                }
                return null;
            } catch (Exception e) {
                throw new KeySelectorException(e);
            }
        }

        /**
         * Ordena a cadeia do certificado folha para a raiz, como exigido por
         * {@link CertificateFactory#generateCertPath(List)}. A folha é o único certificado que não
         * assina nenhum outro certificado do conjunto.
         */
        private static List<X509Certificate> leafFirst(Collection<X509Certificate> certificates)
                throws KeySelectorException {
            List<X509Certificate> remaining = new ArrayList<>(certificates);
            X509Certificate leaf = remaining.stream()
                    .filter(candidate -> remaining.stream().noneMatch(other -> other != candidate
                            && other.getIssuerX500Principal().equals(candidate.getSubjectX500Principal())))
                    .findFirst()
                    .orElseThrow(() -> new KeySelectorException(
                            "Não foi possível identificar o certificado folha em KeyInfo"));

            List<X509Certificate> chain = new ArrayList<>();
            X509Certificate current = leaf;
            while (current != null) {
                chain.add(current);
                remaining.remove(current);
                X509Certificate issuer = current;
                current = remaining.stream()
                        .filter(candidate -> candidate.getSubjectX500Principal()
                                .equals(issuer.getIssuerX500Principal()))
                        .findFirst()
                        .orElse(null);
            }
            return chain;
        }

        private static void validateCertificateChain(
                List<X509Certificate> chain,
                KeyStore trustStore,
                Date signingDate
        ) throws Exception {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            CertPath path = factory.generateCertPath(chain);
            PKIXParameters parameters = new PKIXParameters(trustStore);
            parameters.setDate(signingDate);

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) validator.getRevocationChecker();
            revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL));
            parameters.addCertPathChecker(revocationChecker);

            validator.validate(path, parameters);
            if (!revocationChecker.getSoftFailExceptions().isEmpty()) {
                log.warn("Revogação do certificado NF-e não pôde ser verificada: {}",
                        revocationChecker.getSoftFailExceptions().get(0).getMessage());
            }
        }
    }
}
