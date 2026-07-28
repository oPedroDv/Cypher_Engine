package com.cypher.infrastructure.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataIntegrityCheck implements ApplicationRunner {

    private static final String LEGACY_TENANT = "00000000-0000-0000-0000-000000000001";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM invoice
                WHERE tenant_id = ?::uuid
                  AND legacy_migrated = FALSE
                """, Long.class, LEGACY_TENANT);
        if (count != null && count > 0) {
            log.warn("Encontrados {} registros no tenant sentinel sem revisão/marcação de legado", count);
        }
    }
}
