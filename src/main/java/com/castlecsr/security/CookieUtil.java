package com.castlecsr.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String COOKIE_NAME = "auth_token";
    private static final Duration MAX_AGE = Duration.ofMinutes(30); // igual que expiración del JWT

    // Configurable por perfil: false en local (sin HTTPS), true en prod
    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    /**
     * Se usa ResponseCookie (Spring) en vez de jakarta.servlet.http.Cookie porque
     * este último no soporta el atributo SameSite de forma confiable/portable.
     */
    public ResponseCookie createAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(MAX_AGE)
                .sameSite("Strict") // mitiga CSRF: la cookie no se envía en requests cross-site
                .build();
    }

    public ResponseCookie createExpiredCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ZERO) // elimina la cookie
                .sameSite("Strict")
                .build();
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}