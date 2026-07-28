package com.cypher.infrastructure.http;

import com.cypher.analysis.domain.InvoiceStatus;
import com.cypher.analysis.service.SefazClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Component
@ConditionalOnProperty(name = "sefaz.enabled", havingValue = "true")
public class SefazClientHttp implements SefazClient {

    private static final String PROVIDER = "SEFAZ";
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SefazClientHttp(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            HttpClient externalHttpClient,
            @Value("${sefaz.url}") String baseUrl,
            @Value("${cypher.http.read-timeout:10s}") Duration readTimeout
    ) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(externalHttpClient);
        factory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl).defaultHeader("Accept", "application/json")
                .requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CircuitBreaker(name = "sefaz", fallbackMethod = "fallbackConsultStatus")
    public ConsultationResult consultStatus(String accessKey) {
        String cleanAccessKey = cleanAccessKey(accessKey);
        String body = restClient.get().uri("/{accessKey}", cleanAccessKey).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            log.error("Resposta vazia da SEFAZ para accessKey={}", cleanAccessKey);
            return ConsultationResult.unavailable(PROVIDER, "Resposta vazia da integração SEFAZ");
        }
        try {
            SefazStatusResponse response = objectMapper.readValue(body, SefazStatusResponse.class);
            return ConsultationResult.available(
                    InvoiceStatus.fromSefazCode(response.resolvedStatusCode()), PROVIDER,
                    Objects.requireNonNullElse(response.resolvedMessage(), "Status retornado pela SEFAZ"));
        } catch (JsonProcessingException e) {
            log.error("Resposta SEFAZ inválida para accessKey={}: {}", cleanAccessKey, e.getMessage(), e);
            return ConsultationResult.unavailable(PROVIDER, "Resposta inválida da integração SEFAZ");
        }
    }

    @SuppressWarnings("unused")
    private ConsultationResult fallbackConsultStatus(String accessKey, Throwable cause) {
        log.warn("Fallback SEFAZ acionado para accessKey={} causa={}",
                cleanAccessKey(accessKey), cause.getMessage(), cause);
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
