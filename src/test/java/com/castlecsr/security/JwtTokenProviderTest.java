package com.castlecsr.security;

import com.castlecsr.exception.ExpiredTokenException;
import com.castlecsr.exception.InvalidTokenException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET_BASE64 = generateTestSecret();

    private static String generateTestSecret() {
        byte[] bytes = new byte[64]; // 512 bits, válido para HS512
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecretBase64", TEST_SECRET_BASE64);
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", 1800000L); // 30 min
    }

    @Test
    void generateToken_devuelveTokenNoNuloYConTresPartes() {
        String token = jwtTokenProvider.generateToken("jgomez", "ADMIN");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length); // header.payload.signature
    }

    @Test
    void validateAndGetClaims_conTokenValido_devuelveClaimsCorrectos() {
        String token = jwtTokenProvider.generateToken("jgomez", "ADMIN");

        JWTClaimsSet claims = jwtTokenProvider.validateAndGetClaims(token);

        assertEquals("jgomez", claims.getSubject());
        assertEquals("castlecsr-backend", claims.getIssuer());
        assertDoesNotThrow(() -> assertEquals("ADMIN", claims.getStringClaim("role")));
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());
    }

    @Test
    void validateAndGetClaims_conFirmaInvalida_lanzaInvalidTokenException() {
        String token = jwtTokenProvider.generateToken("jgomez", "ADMIN");

        // Cambiar el secreto simula una firma que no corresponde al token
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecretBase64", generateTestSecret());

        assertThrows(
                InvalidTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims(token)
        );
    }

    @Test
    void validateAndGetClaims_conTokenExpirado_lanzaExpiredTokenException() {
        // Expiración negativa: el token nace ya expirado
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", -1000L);
        String token = jwtTokenProvider.generateToken("jgomez", "ADMIN");

        assertThrows(
                ExpiredTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims(token)
        );
    }

    @Test
    void validateAndGetClaims_conTokenMalformado_lanzaInvalidTokenException() {
        assertThrows(
                InvalidTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims("esto-no-es-un-jwt-valido")
        );
    }
}