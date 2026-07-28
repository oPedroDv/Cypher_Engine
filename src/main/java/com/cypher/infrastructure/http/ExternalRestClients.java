package com.cypher.infrastructure.http;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

final class ExternalRestClients {

    private ExternalRestClients() {}

    static RestClient jsonClient(
            RestClient.Builder builder,
            HttpClient httpClient,
            String baseUrl,
            Duration readTimeout
    ) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return builder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .requestFactory(factory)
                .build();
    }
}
