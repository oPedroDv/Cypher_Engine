package com.cypher.analysis.domain;

import com.cypher.shared.exception.InvalidNFeException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

public class NFeParser {

    private static final int MAX_XML_BYTES = 1024 * 1024;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private NFeParser() {}

    public static NFeData parse(String input) {
        validateInput(input);

        String xml = decodeIfBase64(input);

        if (xml.getBytes(StandardCharsets.UTF_8).length > MAX_XML_BYTES) {
            throw new InvalidNFeException("XML da NF-e excede o tamanho máximo permitido (1MB)");
        }

        try {
            Document document = buildDocument(xml);

            String accessKey = extractAccessKey(document);

            String issuerCnpj = extractNested(document, "emit", "CNPJ", null);
            if (issuerCnpj == null) {
                throw new InvalidNFeException("CNPJ do emitente ausente no XML");
            }

            String issuerName = extractNested(document, "emit", "xNome", "EMITENTE DESCONHECIDO");

            String recipientCnpj = extractNested(document, "dest", "CNPJ", null);
            if (recipientCnpj == null) {
                recipientCnpj = extractNested(document, "dest", "CPF", null);
            }
            if (recipientCnpj == null) {
                throw new InvalidNFeException("CNPJ/CPF do destinatário ausente no XML");
            }

            String recipientName = extractNested(document, "dest", "xNome", "DESTINATARIO DESCONHECIDO");

            BigDecimal totalAmount = extractBigDecimal(document, "vNF",    BigDecimal.ZERO);
            BigDecimal freightAmount = extractBigDecimal(document, "vFrete", BigDecimal.ZERO);
            BigDecimal discountAmount = extractBigDecimal(document, "vDesc",  BigDecimal.ZERO);

             String rawIssueDate = extractOptional(document, "dhEmi",
                    extractOptional(document, "dEmi", null));
            LocalDate issueDate = parseDate(rawIssueDate)
                    .orElseThrow(() -> new InvalidNFeException("Data de emissão ausente ou inválida no XML"));

            String rawDueDate = extractOptional(document, "dVenc", null);
            LocalDate dueDate = parseDate(rawDueDate)
                    .orElse(issueDate.plusDays(30));

            return NFeData.builder()
                    .accessKey(normalizeKey(accessKey))
                    .issuerCnpj(issuerCnpj)
                    .issuerLegalName(issuerName)
                    .recipientCnpj(recipientCnpj)
                    .recipientLegalName(recipientName)
                    .totalAmount(totalAmount)
                    .productsAmount(totalAmount.subtract(freightAmount).add(discountAmount))
                    .freightAmount(freightAmount)
                    .discountAmount(discountAmount)
                    .issueDate(issueDate)
                    .dueDate(dueDate)
                    .status(InvoiceStatus.PENDING)
                    .buildUnsafe();

        } catch (InvalidNFeException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidNFeException("Falha ao processar XML da NF-e: " + e.getMessage());
        }
    }

    private static String extractAccessKey(Document document) {
        NodeList nodes = document.getElementsByTagName("chNFe");
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }

        NodeList infNFeNodes = document.getElementsByTagName("infNFe");
        if (infNFeNodes.getLength() > 0) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) infNFeNodes.item(0);
            String idAttr = element.getAttribute("Id");
            if (idAttr != null && idAttr.startsWith("NFe")) {
                return idAttr.substring(3);
            }
        }

        throw new InvalidNFeException("Tag obrigatória ausente: chNFe (ou atributo Id em infNFe)");
    }

    private static String extractNested(Document document, String parentTag, String childTag, String defaultValue) {
        NodeList parents = document.getElementsByTagName(parentTag);
        if (parents.getLength() > 0) {
            org.w3c.dom.Element parent = (org.w3c.dom.Element) parents.item(0);
            NodeList children = parent.getElementsByTagName(childTag);
            if (children.getLength() > 0) {
                String value = children.item(0).getTextContent().trim();
                return value.isEmpty() ? defaultValue : value;
            }
        }
        return defaultValue;
    }

    private static Optional<LocalDate> parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Optional.empty();
        try {
            if (dateStr.contains("T")) {
                return Optional.of(LocalDate.parse(dateStr, ISO_OFFSET_DATE_TIME));
            }
            return Optional.of(LocalDate.parse(dateStr, ISO_DATE));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void validateInput(String input) {
        if (input == null || input.isBlank()) {
            throw new InvalidNFeException("XML da NF-e não pode ser vazio");
        }
    }

    private static String decodeIfBase64(String input) {
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            String decodedString = new String(decoded, StandardCharsets.UTF_8);
            if (decodedString.contains("<")) {
                return decodedString;
            }
            return input;
        } catch (IllegalArgumentException e) {
            return input;
        }
    }

    private static Document buildDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String extractOptional(Document document, String tag, String defaultValue) {
        NodeList nodes = document.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return defaultValue;
        String value = nodes.item(0).getTextContent().trim();
        return value.isEmpty() ? defaultValue : value;
    }

    private static BigDecimal extractBigDecimal(Document document, String tag, BigDecimal defaultValue) {
        try {
            String value = extractOptional(document, tag, null);
            return value == null ? defaultValue : new BigDecimal(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String normalizeKey(String key) {
        String normalized = key.replaceAll("\\D", "");
        if (normalized.length() != 44) {
            throw new InvalidNFeException("Chave NF-e inválida: esperado 44 dígitos, encontrado " + normalized.length());
        }
        return normalized;
    }
}
