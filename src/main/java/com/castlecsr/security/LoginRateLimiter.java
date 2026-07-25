package com.castlecsr.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter en memoria para POST /api/auth/login (protección contra fuerza bruta).
 * Ventana deslizante simple por IP: tras maxAttempts intentos fallidos dentro de la
 * ventana, la IP queda bloqueada hasta que expire. Un login exitoso limpia el contador.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_IPS = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${castlecsr.security.login.max-attempts:5}") int maxAttempts,
            @Value("${castlecsr.security.login.window-seconds:300}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** true si la IP superó el límite y debe rechazarse con 429. */
    public boolean isBlocked(String ip) {
        Attempts attempts = attemptsByIp.get(ip);
        if (attempts == null) {
            return false;
        }
        if (attempts.windowExpired(window)) {
            attemptsByIp.remove(ip);
            return false;
        }
        return attempts.count >= maxAttempts;
    }

    public void recordFailure(String ip) {
        if (attemptsByIp.size() >= MAX_TRACKED_IPS) {
            attemptsByIp.entrySet().removeIf(e -> e.getValue().windowExpired(window));
        }
        attemptsByIp.compute(ip, (key, current) -> {
            if (current == null || current.windowExpired(window)) {
                return new Attempts(1, Instant.now());
            }
            return new Attempts(current.count + 1, current.windowStart);
        });
    }

    public void recordSuccess(String ip) {
        attemptsByIp.remove(ip);
    }

    /** Segundos restantes de bloqueo (para el header Retry-After). */
    public long retryAfterSeconds(String ip) {
        Attempts attempts = attemptsByIp.get(ip);
        if (attempts == null) {
            return 0;
        }
        long remaining = window.minus(Duration.between(attempts.windowStart, Instant.now())).toSeconds();
        return Math.max(remaining, 1);
    }

    private record Attempts(int count, Instant windowStart) {
        boolean windowExpired(Duration window) {
            return !Instant.now().isBefore(windowStart.plus(window));
        }
    }
}
