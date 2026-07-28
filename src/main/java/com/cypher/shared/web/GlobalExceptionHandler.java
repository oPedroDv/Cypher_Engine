
package com.cypher.shared.web;

import com.cypher.shared.exception.CypherException;
import com.cypher.shared.exception.DuplicateInvoiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("Acesso negado em {}: {}", request.getRequestURI(), ex.getMessage());
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

        log.warn("Exceção de negócio: [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);

        return ResponseEntity.status(ex.getStatus()).body(body(
                ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("Erro de validação: {}", errors);

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Campos inválidos na requisição.", request);
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        log.warn("Erro de validação em {}: {}", request.getRequestURI(), errors);

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Parâmetros inválidos na requisição.", request);
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        log.warn("Requisição malformada em {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Requisição malformada ou com parâmetros inválidos.", request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Argumento inválido em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                ex.getMessage() != null ? ex.getMessage() : "Argumento inválido na requisição.", request));
    }

    @ExceptionHandler({ResponseStatusException.class, ErrorResponseException.class})
    public ResponseEntity<Map<String, Object>> handleErrorResponse(ErrorResponseException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("Erro HTTP {} em {}: {}", status.value(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(status).body(body(status, "HTTP_ERROR",
                ex.getBody().getDetail() != null ? ex.getBody().getDetail() : "Requisição não pôde ser processada.",
                request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Erro interno. Tente novamente em instantes.", request));
    }

    private Map<String, Object> body(
            HttpStatusCode status,
            String errorCode,
            String message,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error_code", errorCode);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
