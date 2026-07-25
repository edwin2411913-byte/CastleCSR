package com.castlecsr.exception;

/** Lanzada cuando una IP supera el límite de intentos de login. Se mapea a HTTP 429. */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Demasiados intentos de login. Intenta de nuevo más tarde.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}