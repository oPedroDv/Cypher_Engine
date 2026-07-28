package com.cypher.analysis.api;

import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.service.AnalysisService;
import com.cypher.infrastructure.persistence.TenantContext;
import com.cypher.infrastructure.security.RequiresScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisListController {

    private final AnalysisService service;

    @GetMapping

    @RequiresScope("analysis:read")
    public ResponseEntity<Page<AnalysisResponse>> listAnalyses(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String riskLevel
    ) {
        log.debug("Listando análises page={} size={} riskLevel={}", page, size, riskLevel);
        return ResponseEntity.ok(service.listAnalyses(TenantContext.getRequired(), page, size, riskLevel));
    }
}
