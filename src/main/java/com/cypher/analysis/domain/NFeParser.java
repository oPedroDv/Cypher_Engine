package com.cypher.analysis.domain;

import com.cypher.shared.exception.InvalidNFeException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;

public class NFeParser {

    private NFeParser() {}

    public static NFeData parse(String xmlBase64) {
        if (xmlBase64 == null || xmlBase64.isBlank()) {
            throw new InvalidNFeException("XML da NF-e não pode ser nulo ou vazio");
        }

        String xml = decodeIfBase64(xmlBase64);

        return NFeData.builder()
                .chaveAcesso(extractChaveFromXml(xml))
                .cnpjEmitente("00000000000000")
                .razaoSocialEmitente("EMITENTE STUB LTDA")
                .cnpjDestinatario("11111111111111")
                .razaoSocialDestinatario("DESTINATARIO STUB LTDA")
                .valorTotal(BigDecimal.valueOf(10000.00))
                .valorProdutos(BigDecimal.valueOf(10000.00))
                .valorFrete(BigDecimal.ZERO)
                .valorDesconto(BigDecimal.ZERO)
                .dataEmissao(LocalDate.now())
                .dataVencimento(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.AUTHORIZED)
                .buildUnsafe();
    }

    private static String extractChaveFromXml(String xml) {
        int start = xml.indexOf("<chNFe>");
        int end   = xml.indexOf("</chNFe>");
        if (start != -1 && end != -1) {
            return xml.substring(start + 7, end).trim();
        }
        String hash = String.valueOf(Math.abs(xml.hashCode()));
        return hash.repeat(5).substring(0, 44);
    }

    private static String decodeIfBase64(String input) {
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return input; 
        }
    }
}