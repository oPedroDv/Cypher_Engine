package com.cypher.analysis.domain;

import com.cypher.shared.exception.InvalidNFeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NFeParserTest {

    private static final String ISSUER_CNPJ = "11222333000181";
    private static final String RECIPIENT_CNPJ = cnpj("334445550001");
    private static final String ACCESS_KEY = accessKey("352606" + ISSUER_CNPJ + "55001000001234100001234");

    private final NFeParser parser = new NFeParser(false);

    @Test
    void parsesValidIdentifiersAndMonetaryTotals() {
        NFeData result = parser.parse(xml(ACCESS_KEY, ISSUER_CNPJ, "100.00", "100.00", "0.00", "0.00"));

        assertThat(result.getAccessKey()).isEqualTo(ACCESS_KEY);
        assertThat(result.getIssuerCnpj()).isEqualTo(ISSUER_CNPJ);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getProductsAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsInvalidAccessKeyCheckDigit() {
        String invalid = ACCESS_KEY.substring(0, 43) + (ACCESS_KEY.endsWith("0") ? "1" : "0");
        assertThatThrownBy(() -> parser.parse(xml(invalid, ISSUER_CNPJ, "100.00", "100.00", "0", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("dígito verificador");
    }

    @Test
    void rejectsIssuerThatDoesNotMatchAccessKey() {
        assertThatThrownBy(() -> parser.parse(xml(ACCESS_KEY, RECIPIENT_CNPJ, "100.00", "100.00", "0", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("diverge");
    }

    @Test
    void rejectsUnsignedInvoiceWhenSignatureIsRequired() {
        NFeParser strictParser = new NFeParser(true);
        assertThatThrownBy(() -> strictParser.parse(xml(ACCESS_KEY, ISSUER_CNPJ, "100.00", "100.00", "0", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("Assinatura digital");
    }

    @Test
    void requiresTrustStoreWhenSignatureValidationIsEnabled() {
        assertThatThrownBy(() -> new NFeParser(true, "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NFE_TRUST_STORE_PATH");
    }

    @Test
    void rejectsMalformedOrNegativeMoney() {
        assertThatThrownBy(() -> parser.parse(xml(ACCESS_KEY, ISSUER_CNPJ, "abc", "100.00", "0", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("vNF");
        assertThatThrownBy(() -> parser.parse(xml(ACCESS_KEY, ISSUER_CNPJ, "100.00", "100.00", "-1", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("vFrete");
    }

    @Test
    void rejectsInconsistentTotal() {
        assertThatThrownBy(() -> parser.parse(xml(ACCESS_KEY, ISSUER_CNPJ, "90.00", "100.00", "0", "0")))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("incompatível");
    }

    private static String xml(
            String accessKey,
            String issuerCnpj,
            String total,
            String products,
            String freight,
            String discount
    ) {
        return """
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe%s" versao="4.00">
                    <ide><dhEmi>2026-06-01T10:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>%s</CNPJ><xNome>Emitente</xNome></emit>
                    <dest><CNPJ>%s</CNPJ><xNome>Destinatário</xNome></dest>
                    <total><ICMSTot>
                      <vProd>%s</vProd><vFrete>%s</vFrete><vDesc>%s</vDesc><vNF>%s</vNF>
                    </ICMSTot></total>
                  </infNFe>
                </NFe>
                """.formatted(accessKey, issuerCnpj, RECIPIENT_CNPJ, products, freight, discount, total);
    }

    private static String accessKey(String first43Digits) {
        int weight = 2;
        int sum = 0;
        for (int i = first43Digits.length() - 1; i >= 0; i--) {
            sum += Character.digit(first43Digits.charAt(i), 10) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? 0 : 11 - remainder;
        return first43Digits + checkDigit;
    }

    private static String cnpj(String first12Digits) {
        StringBuilder digits = new StringBuilder(first12Digits);
        for (int position = 12; position <= 13; position++) {
            int sum = 0;
            int weight = position == 12 ? 5 : 6;
            for (int i = 0; i < position; i++) {
                sum += Character.digit(digits.charAt(i), 10) * weight;
                weight = weight == 2 ? 9 : weight - 1;
            }
            int remainder = sum % 11;
            digits.append(remainder < 2 ? 0 : 11 - remainder);
        }
        return digits.toString();
    }
}
