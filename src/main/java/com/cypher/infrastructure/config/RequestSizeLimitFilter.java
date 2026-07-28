package com.cypher.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxRequestBytes;

    public RequestSizeLimitFilter(@Value("${cypher.http.max-request-bytes:1500000}") long maxRequestBytes) {
        if (maxRequestBytes <= 0 || maxRequestBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("cypher.http.max-request-bytes deve estar entre 1 e 2147483646");
        }
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxRequestBytes) {
            writePayloadTooLarge(response);
            return;
        }

        try {
            filterChain.doFilter(new LimitedRequest(request, maxRequestBytes), response);
        } catch (Exception ex) {
            if (hasPayloadTooLargeCause(ex) && !response.isCommitted()) {
                response.resetBuffer();
                writePayloadTooLarge(response);
                return;
            }
            throw ex;
        }
    }

    private boolean hasPayloadTooLargeCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PayloadTooLargeException) return true;
            current = current.getCause();
        }
        return false;
    }

    private void writePayloadTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error_code\":\"PAYLOAD_TOO_LARGE\",\"message\":\"Requisição excede o tamanho máximo permitido\"}");
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final ServletInputStream inputStream;

        private LimitedRequest(HttpServletRequest request, long maxBytes) throws IOException {
            super(request);
            byte[] body = request.getInputStream().readNBytes((int) maxBytes + 1);
            if (body.length > maxBytes) throw new PayloadTooLargeException();
            this.inputStream = new CachedServletInputStream(body);
        }

        @Override
        public ServletInputStream getInputStream() {
            return inputStream;
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
    }

    private static final class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        private CachedServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return delegate.read(buffer, offset, length);
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }

    private static final class PayloadTooLargeException extends IOException {
        private PayloadTooLargeException() {
            super("Request body exceeds configured limit");
        }
    }
}
