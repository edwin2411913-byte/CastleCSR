package com.castlecsr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rechaza con 413 los requests a /api/** cuyo body supere el tamaño máximo permitido.
 * Los payloads legítimos de la aplicación (login, generación de CSR) son de pocos KB.
 */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestSizeLimitFilter(
            @Value("${castlecsr.security.max-payload-bytes:65536}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            response.setStatus(413);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":413,\"error\":\"Payload Too Large\",\"message\":\"El cuerpo de la petición excede el tamaño máximo permitido\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}