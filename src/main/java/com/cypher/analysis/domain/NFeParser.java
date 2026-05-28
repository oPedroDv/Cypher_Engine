package com.cypher.analysis.domain;

import com.cypher.shared.exception.InvalidNFeException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

public class NFeParser {

    private NFeParser() {
    }

    public static NFeData parse(String input) {
        validateInput(input);

        String xml = decodeIfBase64(input);

        try {
            Document document = buildDocument(xml);

            String accessKey = extractRequired(document, "chNFe");
            String issuer = extractOptional(document, "xNome", "EMITENTE DESCONHECIDO");

            BigDecimal totalAmount = extractBigDecimal(document, "vNF", BigDecimal.ZERO);

            return NFeData.builder()
                    .accessKey(normalizeKey(accessKey))
                    .issuerCnpj(extractOptional(document, "CNPJ", "00000000000000"))
                    .issuerLegalName(issuer)
                    .recipientCnpj("11111111111111")
                    .recipientLegalName("DESTINATARIO")
                    .totalAmount(totalAmount)
                    .productsAmount(totalAmount)
                    .freightAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .issueDate(LocalDate.now())
                    .dueDate(LocalDate.now().plusDays(30))
                    .status(InvoiceStatus.AUTHORIZED)
                    .buildUnsafe();

        } catch (Exception e) {
            throw new InvalidNFeException("Falha ao processar XML da NF-e: " + e.getMessage());
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

    private static String extractRequired(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);

        if (nodes.getLength() == 0) {
            throw new InvalidNFeException("Tag obrigatória ausente: " + tag);
        }

        return nodes.item(0).getTextContent().trim();
    }

    private static String extractOptional(Document document, String tag, String defaultValue) {
        NodeList nodes = document.getElementsByTagName(tag);

        if (nodes.getLength() == 0) {
            return defaultValue;
        }

        return nodes.item(0).getTextContent().trim();
    }

    private static BigDecimal extractBigDecimal(Document document, String tag, BigDecimal defaultValue) {
        try {
            String value = extractOptional(document, tag, null);

            if (value == null) {
                return defaultValue;
            }

            return new BigDecimal(value);

        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String normalizeKey(String key) {
        String normalized = key.replaceAll("\\D", "");

        if (normalized.length() != 44) {
            throw new InvalidNFeException("Chave NF-e inválida");
        }

        return normalized;
    }
}
