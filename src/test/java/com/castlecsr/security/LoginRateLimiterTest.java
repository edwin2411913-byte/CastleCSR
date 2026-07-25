package com.castlecsr.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    private static final String IP = "10.0.0.1";
    private static final String OTRA_IP = "10.0.0.2";

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter(3, 300);
    }

    @Test
    void ipSinIntentos_noEstaBloqueada() {
        assertThat(limiter.isBlocked(IP)).isFalse();
    }

    @Test
    void ipBajoElLimite_noEstaBloqueada() {
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        assertThat(limiter.isBlocked(IP)).isFalse();
    }

    @Test
    void ipQueAlcanzaElLimite_quedaBloqueada() {
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        assertThat(limiter.isBlocked(IP)).isTrue();
    }

    @Test
    void bloqueoEsPorIp_otraIpNoAfectada() {
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        assertThat(limiter.isBlocked(OTRA_IP)).isFalse();
    }

    @Test
    void loginExitoso_limpiaContador() {
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        limiter.recordSuccess(IP);
        limiter.recordFailure(IP);
        assertThat(limiter.isBlocked(IP)).isFalse();
    }

    @Test
    void ipBloqueada_reportaSegundosDeEspera() {
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        limiter.recordFailure(IP);
        assertThat(limiter.retryAfterSeconds(IP)).isBetween(1L, 300L);
    }

    @Test
    void ventanaExpirada_desbloqueaLaIp() {
        LoginRateLimiter corto = new LoginRateLimiter(1, 0);
        corto.recordFailure(IP);
        // Con ventana de 0 segundos el bloqueo expira inmediatamente
        assertThat(corto.isBlocked(IP)).isFalse();
    }
}