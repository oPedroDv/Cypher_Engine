package com.cypher.infrastructure.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Duration;

@Slf4j
@Configuration
public class TlsConfig {

    @Bean
    public HttpClient externalHttpClient(
            @Value("${cypher.http.check-revocation:true}") boolean checkRevocation,
            @Value("${cypher.http.connect-timeout:3s}") Duration connectTimeout
    ) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(connectTimeout);
        SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        builder.sslParameters(sslParameters);

        if (!checkRevocation) {
            log.warn("Verificação de revogação TLS desabilitada por configuração");
            return builder.build();
        }

        try {
            TrustManagerFactory defaults = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            defaults.init((KeyStore) null);
            X509TrustManager defaultTrustManager = Arrays.stream(defaults.getTrustManagers())
                    .filter(X509TrustManager.class::isInstance)
                    .map(X509TrustManager.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Trust manager X.509 padrão indisponível"));

            Set<TrustAnchor> anchors = Arrays.stream(defaultTrustManager.getAcceptedIssuers())
                    .map(certificate -> new TrustAnchor(certificate, null))
                    .collect(Collectors.toUnmodifiableSet());
            PKIXBuilderParameters parameters = new PKIXBuilderParameters(anchors, new X509CertSelector());
            parameters.setRevocationEnabled(true);

            TrustManagerFactory pkix = TrustManagerFactory.getInstance("PKIX");
            pkix.init(new CertPathTrustManagerParameters(parameters));
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, pkix.getTrustManagers(), new SecureRandom());

            Security.setProperty("ocsp.enable", "true");
            System.setProperty("com.sun.net.ssl.checkRevocation", "true");
            return builder.sslContext(sslContext).build();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível configurar validação TLS com revogação", e);
        }
    }
}
