package com.cypher.infrastructure.http;

import com.cypher.analysis.domain.InvoiceStatus;
import com.cypher.analysis.service.SefazClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SefazHttpClient implements SefazClient {

    private static final String CIRCUIT_BREAKER_NAME = "sefaz";

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackConsultStats")
    public InvoiceStatus consultStats(String accessKey) {
        log.debug("Consultanddo status NF-e na SEFAZ accessKey={}", accessKey);
        log.warn("SefazHttpClient aind é stub - retornando AUTHORIZED para accessKey={}", accessKey);
        return InvoiceStatus.AUTHORIZED;
    }

    @SuppressWarnings("unused")
    private InvoiceStatus fallbackConsultStats(String accessKey, Throwable cause) {
        log.warn("Fallback SEFAZ acionado para accessKey={} causa={}", accessKey, cause.getMessage());
        return InvoiceStatus.ERROR;
    }
}