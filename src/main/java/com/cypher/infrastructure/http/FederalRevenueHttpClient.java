package com.cypher.infrastructure.http;

import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.service.FederalRevenueClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class FederalRevenueHttpClient implements FederalRevenueClient {

    private static final Logger log = LoggerFactory.getLogger(FederalRevenueHttpClient.class);
    private static final String BRASIL_API_URL = "https://brasilapi.com.br/api/cnpj/v1/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FederalRevenueHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CnpjData query(String cnpj) {
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BRASIL_API_URL + cleanCnpj))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                BrasilApiCnpjResponse apiResponse = objectMapper.readValue(response.body(), BrasilApiCnpjResponse.class);
                return mapToCnpjData(cleanCnpj, apiResponse);
            }

            if (response.statusCode() == 404) {
                log.warn("CNPJ não encontrado na Receita Federal: {}", cleanCnpj);
                return new CnpjData(cleanCnpj, "EMPRESA NÃO ENCONTRADA", null, CnpjStatus.NULLIFIED);
            }

            log.error("BrasilAPI retornou status inesperado {} para CNPJ {}", response.statusCode(), cleanCnpj);
            return CnpjData.unknown(cleanCnpj);

        } catch (Exception e) {
            log.error("Erro ao consultar BrasilAPI para CNPJ {}: {}", cleanCnpj, e.getMessage());
            return CnpjData.unknown(cleanCnpj);
        }
    }

    private CnpjData mapToCnpjData(String cnpj, BrasilApiCnpjResponse apiResponse) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BrasilApiCnpjResponse(
            @JsonProperty("cnpj") String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("descricao_situacao_cadastral") String descricaoSituacaoCadastral,
            @JsonProperty("situacao_cadastral") Integer situacaoCadastral
    ) {}
}