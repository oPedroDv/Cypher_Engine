package com.cypher.outcome.api;

import com.cypher.outcome.api.dto.OutcomeRequest;
import com.cypher.outcome.service.OutcomeService;
import com.cypher.infrastructure.persistence.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class OutcomeController {

    private final OutcomeService outcomeService;

    @PostMapping("/{analysisId}/outcome")
    public ResponseEntity<Void> registerOutcome(
            @PathVariable UUID analysisId,
            @Valid @RequestBody OutcomeRequest request
    ) {
        UUID tenantId = TenantContext.get();
        log.info("Registrando outcome analysisId={} outcome={} tenant={}", analysisId, request.outcome(), tenantId);

        outcomeService.register(analysisId, request, tenantId);

        return ResponseEntity.noContent().build();
    }
}