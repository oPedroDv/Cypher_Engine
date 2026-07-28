package com.cypher.shared.util;

public final class Digits {

    private Digits() {}

    public static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
