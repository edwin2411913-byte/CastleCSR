package com.castlecsr.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Da formato JSON consistente a los 401, igual al que usa GlobalExceptionHandler
 * para el resto de errores. Las navegaciones del navegador (Accept: text/html)
 * se redirigen a la página de login en lugar de recibir JSON.
 */
@Component
public class
     JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String accept = request.getHeader("Accept");
        boolean isApiRequest = request.getRequestURI().startsWith("/api/");

        if (!isApiRequest && accept != null && accept.contains("text/html")) {
            response.sendRedirect("/login.html");
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Autenticación requerida\"}"
        );
    }
}