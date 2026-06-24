package com.cypher.infrastructure.http;

import com.cypher.analysis.domain.InvoiceStatus;
import com.cypher.analysis.service.SefazClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Component
public class SefazHttpClient implements SefazClient {

    private static final String CIRCUIT_BREAKER_NAME = "sefaz";
    private static final String PROVIDER = "SEFAZ";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public SefazHttpClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${sefaz.url:}") String baseUrl,
            @Value("${sefaz.enabled:false}") boolean enabled,
            @Value("${cypher.http.connect-timeout:3s}") Duration connectTimeout,
            @Value("${cypher.http.read-timeout:10s}") Duration readTimeout
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled && baseUrl != null && !baseUrl.isBlank();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder
                .baseUrl(baseUrl == null || baseUrl.isBlank() ? "http://localhost" : baseUrl)
                .defaultHeader("Accept", "application/json")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackConsultStatus")
    public ConsultationResult consultStatus(String accessKey) {
        String cleanAccessKey = cleanAccessKey(accessKey);

        if (!enabled) {
            log.warn("Integração SEFAZ não configurada. accessKey={}", cleanAccessKey);
            return ConsultationResult.notConfigured(PROVIDER, "Integração SEFAZ não configurada neste ambiente");
        }

        log.debug("Consultando status NF-e na SEFAZ accessKey={}", cleanAccessKey);

        String body = restClient.get()
                .uri("/{accessKey}", cleanAccessKey)
                .retrieve()
                .body(String.class);

        try {
            SefazStatusResponse response = objectMapper.readValue(body, SefazStatusResponse.class);
            InvoiceStatus status = InvoiceStatus.fromSefazCode(response.resolvedStatusCode());
            return ConsultationResult.available(
                    status,
                    PROVIDER,
                    Objects.requireNonNullElse(response.resolvedMessage(), "Status retornado pela SEFAZ")
            );
        } catch (Exception e) {
            log.error("Erro ao deserializar resposta SEFAZ para accessKey={}: {}", cleanAccessKey, e.getMessage());
            return ConsultationResult.unavailable(PROVIDER, "Resposta inválida da integração SEFAZ");
        }
    }

    @SuppressWarnings("unused")
    private ConsultationResult fallbackConsultStatus(String accessKey, Throwable cause) {
        log.warn("Fallback SEFAZ acionado para accessKey={} causa={}", cleanAccessKey(accessKey), cause.getMessage());
        return ConsultationResult.unavailable(PROVIDER, cause.getMessage());
    }

    private String cleanAccessKey(String accessKey) {
        return accessKey == null ? "" : accessKey.replaceAll("[^0-9]", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SefazStatusResponse(
            @JsonProperty("cStat") String cStat,
            @JsonProperty("statusCode") String code,
            @JsonProperty("status") String status,
            @JsonProperty("xMotivo") String xMotivo,
            @JsonProperty("message") String providerMessage
    ) {
        String resolvedStatusCode() {
            if (cStat != null && !cStat.isBlank()) return cStat;
            if (code != null && !code.isBlank()) return code;
            return status;
        }

        String resolvedMessage() {
            if (xMotivo != null && !xMotivo.isBlank()) return xMotivo;
            return providerMessage;
        }
    }
}
