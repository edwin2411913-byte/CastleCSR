package com.castlecsr.exception;

import com.castlecsr.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadCredentials_devuelve401ConMensajeGenerico() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("detalle interno"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
        // El mensaje no debe filtrar si falló el username o el password
        assertFalse(response.getBody().getMessage().toLowerCase().contains("username"));
    }

    @Test
    void handleAccessDenied_devuelve403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("sin permiso"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Acceso denegado", response.getBody().getMessage());
    }

    @Test
    void handleInvalidToken_devuelve401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidToken(new InvalidTokenException("firma inválida"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Token inválido", response.getBody().getMessage());
    }

    @Test
    void handleExpiredToken_devuelve401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleExpiredToken(new ExpiredTokenException("expiró"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Token expirado", response.getBody().getMessage());
    }
}