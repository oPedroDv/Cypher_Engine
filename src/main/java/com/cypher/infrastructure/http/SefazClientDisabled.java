package com.cypher.infrastructure.http;

import com.cypher.analysis.service.SefazClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "sefaz.enabled", havingValue = "false", matchIfMissing = true)
public class SefazClientDisabled implements SefazClient {
    @Override
    public ConsultationResult consultStatus(String accessKey) {
        log.warn("Integração SEFAZ desabilitada — status oficial não será consultado");
        return ConsultationResult.notConfigured("SEFAZ", "Integração SEFAZ não configurada neste ambiente");
    }
}
