package com.cypher.shared.util;

public final class CnpjValidator {

    private static final int CNPJ_LENGTH = 14;

    private CnpjValidator() {}

    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;

        String digits = cnpj.replaceAll("[^0-9]", "");
        if (digits.length() != CNPJ_LENGTH) return false;
        if (isAllSameDigits(digits)) return false;

        return checkDigit(digits, 12) && checkDigit(digits, 13);
    }

    public static String strip(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("[^0-9]", "");
    }

    private static boolean checkDigit(String digits, int position) {
        int sum    = 0;
        int weight = position == 12 ? 5 : 6;

        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weight;
            weight = weight == 2 ? 9 : weight - 1;
        }

        int remainder   = sum % 11;
        int expectedDv  = remainder < 2 ? 0 : 11 - remainder;
        int actualDv    = Character.getNumericValue(digits.charAt(position));

        return expectedDv == actualDv;
    }

    private static boolean isAllSameDigits(String digits) {
        return digits.chars().distinct().count() == 1;
    }
}