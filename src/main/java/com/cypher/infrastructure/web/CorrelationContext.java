package com.cypher.infrastructure.web;


public final class CorrelationContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationContext() {}

    public static void set(String correlationId) { CURRENT.set(correlationId); }
    public static String get() { return CURRENT.get(); }
    public static String getOrCreate() {
        String current = CURRENT.get();
        return current != null ? current : java.util.UUID.randomUUID().toString();
    }
    public static void clear() { CURRENT.remove(); }
}
