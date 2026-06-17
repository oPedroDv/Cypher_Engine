package com.cypher.analysis.service;

import com.cypher.analysis.domain.InvoiceStatus;

public interface SefazClient {
    ConsultationResult consultStatus(String accessKey);

    record ConsultationResult(
            InvoiceStatus status,
            boolean sourceUnavailable,
            boolean notConfigured,
            String provider,
            String message
    ) {
        public static ConsultationResult available(InvoiceStatus status, String provider, String message) {
            return new ConsultationResult(status, false, false, provider, message);
        }

        public static ConsultationResult unavailable(String provider, String message) {
            return new ConsultationResult(InvoiceStatus.ERROR, true, false, provider, message);
        }

        public static ConsultationResult notConfigured(String provider, String message) {
            return new ConsultationResult(null, true, true, provider, message);
        }
    }
}
