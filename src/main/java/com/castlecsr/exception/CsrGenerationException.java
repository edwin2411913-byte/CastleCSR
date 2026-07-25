package com.castlecsr.exception;

public class CsrGenerationException extends RuntimeException {

    public CsrGenerationException(String message) {
        super(message);
    }

    public CsrGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}