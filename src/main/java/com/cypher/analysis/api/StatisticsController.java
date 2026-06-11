package com.cypher.analysis.api;

import com.cypher.analysis.api.dto.StatisticsResponse;
import com.cypher.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final AnalysisService service;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics() {
        log.debug("Consultando estatísticas de análises");
        return ResponseEntity.ok(service.getStatistics());
    }
}
