
package com.cypher.shared.web;

import com.cypher.shared.exception.CypherException;
import com.cypher.shared.exception.DuplicateInvoiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "access_denied"));
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleCypherException(CypherException ex, HttpServletRequest request) {
        if (ex instanceof DuplicateInvoiceException dup) {
            log.warn("NF-e duplicada detectada. Chave: {}. Análise existente: {}", dup.getNfeKey(), dup.getExistingAnalysisId());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", ex.getStatus().value());
            body.put("error_code", ex.getErrorCode());
            body.put("message", ex.getMessage());
            if (dup.getExistingAnalysisId() != null) {
                body.put("existing_analysis_id", dup.getExistingAnalysisId());
            }
            body.put("path", request.getRequestURI());
            return ResponseEntity.status(ex.getStatus()).body(body);
        }

        log.warn("Exceção de negócio: [{}] {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "timestamp",  Instant.now().toString(),
                "status",     ex.getStatus().value(),
                "error_code", ex.getErrorCode(),
                "message",    ex.getMessage(),
                "path",       request.getRequestURI()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Erro de validação: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp",  Instant.now().toString(),
                "status",     HttpStatus.BAD_REQUEST.value(),
                "error_code", "VALIDATION_ERROR",
                "message",    "Campos inválidos na requisição.",
                "errors",     errors,
                "path",       request.getRequestURI()
        ));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp",  Instant.now().toString(),
                "status",     HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error_code", "INTERNAL_ERROR",
                "message",    "Erro interno. Tente novamente em instantes.",
                "path",       request.getRequestURI()
        ));
    }
}
