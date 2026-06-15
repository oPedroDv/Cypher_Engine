package com.cypher.analysis.service;

import com.cypher.analysis.domain.InvoiceStatus;

public interface SefazClient {
    ConsultationResult consultStatus(String accessKey);

    record ConsultationResult(
            InvoiceStatus status,
            boolean sourceUnavailable,
            String provider,
            String message
    ) {
        public static ConsultationResult available(InvoiceStatus status, String provider, String message) {
            return new ConsultationResult(status, false, provider, message);
        }

        public static ConsultationResult unavailable(String provider, String message) {
            return new ConsultationResult(InvoiceStatus.ERROR, true, provider, message);
        }
    }
}
