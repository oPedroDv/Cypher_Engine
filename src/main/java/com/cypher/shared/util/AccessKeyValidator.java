package com.cypher.shared.util;

public final class AccessKeyValidator {

    private static final int ACCESS_KEY_LENGTH = 44;

    private AccessKeyValidator() {}

    public static boolean isValid(String accessKey) {
        if (accessKey == null) return false;

        String digits = accessKey.replaceAll("[^0-9]", "");
        if (digits.length() != ACCESS_KEY_LENGTH) return false;

        return checkDigit(digits);
    }

    public static String extractUf(String accessKey) {
        if (accessKey == null || accessKey.length() < 2) return null;
        return accessKey.substring(0, 2);
    }

    public static String extractIssuerCnpj(String accessKey) {
        if (accessKey == null || accessKey.length() < 20) return null;
        return accessKey.substring(6, 20);
    }

    private static boolean checkDigit(String digits) {
        int[] weights = {2, 3, 4, 5, 6, 7, 8, 9};
        int sum = 0;

        for (int i = 0; i < ACCESS_KEY_LENGTH - 1; i++) {
            int weight = weights[(ACCESS_KEY_LENGTH - 2 - i) % weights.length];
            sum += Character.getNumericValue(digits.charAt(i)) * weight;
        }

        int remainder = sum % 11;
        int expectedDv = remainder < 2 ? 0 : 11 - remainder;
        int actualDv   = Character.getNumericValue(digits.charAt(ACCESS_KEY_LENGTH - 1));

        return expectedDv == actualDv;
    }
}