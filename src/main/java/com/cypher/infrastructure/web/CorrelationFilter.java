package com.cypher.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied != null && !supplied.isBlank() && supplied.length() <= 128
                ? supplied
                : UUID.randomUUID().toString();
        try {
            CorrelationContext.set(correlationId);
            response.setHeader(HEADER, correlationId);
            chain.doFilter(request, response);
        } finally {
            CorrelationContext.clear();
        }
    }
}
