package com.castlecsr.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieUtilTest {

    private CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {
        cookieUtil = new CookieUtil();
    }

    @Test
    void createAuthCookie_conSecureFalse_generaCookieConAtributosCorrectos() {
        ReflectionTestUtils.setField(cookieUtil, "cookieSecure", false);

        ResponseCookie cookie = cookieUtil.createAuthCookie("token-de-prueba");

        assertEquals("auth_token", cookie.getName());
        assertEquals("token-de-prueba", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(1800, cookie.getMaxAge().getSeconds()); // 30 minutos
    }

    @Test
    void createAuthCookie_conSecureTrue_marcaCookieComoSecure() {
        ReflectionTestUtils.setField(cookieUtil, "cookieSecure", true);

        ResponseCookie cookie = cookieUtil.createAuthCookie("token-de-prueba");

        assertTrue(cookie.isSecure());
    }

    @Test
    void createExpiredCookie_tieneMaxAgeCero() {
        ResponseCookie cookie = cookieUtil.createExpiredCookie();

        assertEquals(0, cookie.getMaxAge().getSeconds());
        assertEquals("", cookie.getValue());
    }

    @Test
    void extractToken_conCookiePresente_devuelveElValor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("auth_token", "abc123"));

        String token = cookieUtil.extractToken(request);

        assertEquals("abc123", token);
    }

    @Test
    void extractToken_sinCookies_devuelveNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(cookieUtil.extractToken(request));
    }

    @Test
    void extractToken_conOtrasCookiesPeroSinAuthToken_devuelveNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otra_cookie", "valor"));

        assertNull(cookieUtil.extractToken(request));
    }
}