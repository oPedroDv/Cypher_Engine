package com.cypher.infrastructure.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Achado 5.16 da auditoria (ALTO, Onda 0 #2): {@code REDIS_PASSWORD} tinha
 * default vazio sem nenhuma validação, ou seja, um deploy fora do profile
 * "dev" que esquecesse de configurar a senha subia normalmente com Redis
 * sem autenticação — mesmo tipo de risco dos itens 4.1/4.4 (segredo default
 * silencioso).
 *
 * Falha rápido no boot (antes de servir tráfego) quando o profile ativo não
 * é "dev" e nenhuma senha de Redis foi configurada. Em "dev", Redis local
 * sem senha continua permitido (é o que o docker-compose local sobe).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSecurityStartupCheck implements ApplicationRunner {

    private final Environment environment;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Override
    public void run(ApplicationArguments args) {
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));

        if (!isDev && (redisPassword == null || redisPassword.isBlank())) {
            throw new IllegalStateException(
                    "REDIS_PASSWORD não configurado fora do profile 'dev'. " +
                    "Redis sem autenticação em ambiente não-dev é um risco crítico " +
                    "(achado 5.16 da auditoria) — configure REDIS_PASSWORD antes de subir.");
        }

        if (isDev && (redisPassword == null || redisPassword.isBlank())) {
            log.warn("Redis sem senha (profile dev) — aceitável apenas em desenvolvimento local.");
        }
    }
}
