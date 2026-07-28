package com.cypher.infrastructure.http;

import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.service.FederalRevenueClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.net.http.HttpClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "receita-federal.enabled", havingValue = "true", matchIfMissing = true)
public class FederalRevenueHttpClient implements FederalRevenueClient {

    private static final String CIRCUIT_BREAKER_NAME = "receita-federal";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FederalRevenueHttpClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            HttpClient externalHttpClient,
            @Value("${receita-federal.url:https://brasilapi.com.br/api/cnpj/v1}") String baseUrl,
            @Value("${cypher.http.read-timeout:10s}") Duration readTimeout
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(externalHttpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackQuery")
    public CnpjData query(String cnpj) {
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        log.debug("Consultando CNPJ {} na Receita Federal via BrasilAPI", cleanCnpj);

        String body = restClient.get()
                .uri("/{cnpj}", cleanCnpj)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new CnpjNotFoundException(cleanCnpj);
                })
                .body(String.class);

        if (body == null || body.isBlank()) {
            log.error("Resposta vazia da BrasilAPI para CNPJ {}", cleanCnpj);
            return CnpjData.unknown(cleanCnpj);
        }

        try {
            BrasilApiCnpjResponse apiResponse = objectMapper.readValue(body, BrasilApiCnpjResponse.class);
            return mapToCnpjData(cleanCnpj, apiResponse);
        } catch (JsonProcessingException e) {
            log.error("Erro ao deserializar resposta BrasilAPI para CNPJ {}: {}", cleanCnpj, e.getMessage(), e);
            return CnpjData.unknown(cleanCnpj);
        }
    }

    private CnpjData fallbackQuery(String cnpj, Throwable cause) {
        if (cause instanceof CnpjNotFoundException) {
            log.warn("CNPJ não encontrado na Receita Federal: {}", cnpj);
            return new CnpjData(cnpj, "EMPRESA NÃO ENCONTRADA", null, CnpjStatus.NULLIFIED);
        }
        log.warn("Fallback Receita Federal acionado para CNPJ={} causa={}", cnpj, cause.getMessage(), cause);
        return CnpjData.unknown(cnpj);
    }

    private CnpjData mapToCnpjData(String cnpj, BrasilApiCnpjResponse apiResponse) {
        if (apiResponse.situacaoCadastral() == null) {
            log.warn("Resposta da BrasilAPI sem situacao_cadastral para CNPJ {}", cnpj);
            return new CnpjData(cnpj, apiResponse.razaoSocial(), apiResponse.nomeFantasia(), CnpjStatus.UNKNOWN);
        }

        CnpjStatus status = switch (apiResponse.situacaoCadastral()) {
            case 2 -> CnpjStatus.ACTIVE;
            case 3 -> CnpjStatus.SUSPENDED;
            case 4 -> CnpjStatus.UNFIT;
            case 8 -> CnpjStatus.CLOSED;
            case 1 -> CnpjStatus.NULLIFIED;
            default -> CnpjStatus.UNKNOWN;
        };
        return new CnpjData(cnpj, apiResponse.razaoSocial(), apiResponse.nomeFantasia(), status);
    }

    private static class CnpjNotFoundException extends RestClientException {
        CnpjNotFoundException(String cnpj) {
            super("CNPJ não encontrado: " + cnpj);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BrasilApiCnpjResponse(
            @JsonProperty("cnpj") String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral,
            @JsonProperty("situacao_cadastral") Integer situacaoCadastral
    ) {}
}
