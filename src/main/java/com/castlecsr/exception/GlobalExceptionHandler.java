package com.castlecsr.exception;

import com.castlecsr.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Los datos proporcionados no son válidos",
                request.getDescription(false).replace("uri=", ""),
                validationErrors
        );

        logger.warn("Validation error: {}", validationErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "El recurso solicitado no existe",
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ha ocurrido un error en el servidor",
                request.getDescription(false).replace("uri=", "")
        );

        logger.error("Unexpected error: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // Mensaje genérico: no revelar si falló el username o el password
        ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Credenciales inválidas");
        return ResponseEntity.status(401).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(403, "Forbidden", "Acceso denegado");
        return ResponseEntity.status(403).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Token inválido");
        return ResponseEntity.status(401).body(error);
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredToken(ExpiredTokenException ex) {
        ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Token expirado");
        return ResponseEntity.status(401).body(error);
    }

    @ExceptionHandler(CsrGenerationException.class)
    public ResponseEntity<ErrorResponse> handleCsrGeneration(CsrGenerationException ex) {
        ErrorResponse error = new ErrorResponse(400, "Bad Request", ex.getMessage());
        logger.warn("CSR generation error: {}", ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(CryptographyException.class)
    public ResponseEntity<ErrorResponse> handleCryptography(CryptographyException ex) {
        // No exponer detalles internos de la librería criptográfica
        ErrorResponse error = new ErrorResponse(500, "Internal Server Error", "Error criptográfico");
        logger.error("Cryptography error: ", ex);
        return ResponseEntity.status(500).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        // 404 también cuando el recurso pertenece a otro usuario: no revelar su existencia
        ErrorResponse error = new ErrorResponse(404, "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado");
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        ErrorResponse error = new ErrorResponse(429, "Too Many Requests", ex.getMessage());
        logger.warn("Login rate limit exceeded");
        return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage() != null ? ex.getMessage() : "Argumento inválido",
                request.getDescription(false).replace("uri=", "")
        );

        logger.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}