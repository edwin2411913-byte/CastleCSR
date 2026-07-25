package com.castlecsr.security;

import com.castlecsr.exception.ExpiredTokenException;
import com.castlecsr.exception.InvalidTokenException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecretBase64;

    @Value("${jwt.expiration-ms:1800000}") // 30 minutos por defecto
    private long expirationMs;

    private static final String ISSUER = "castlecsr-backend";

    private byte[] secretBytes() {
        return Base64.getDecoder().decode(jwtSecretBase64);
    }

    /** Genera un token JWT firmado con HS512 a partir del username y rol del usuario. */
    public String generateToken(String username, String rol) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS512)
                    .type(JOSEObjectType.JWT)
                    .build();

            Date now = new Date();
            Date expiry = new Date(now.getTime() + expirationMs);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)          // sub
                    .issueTime(now)             // iat
                    .expirationTime(expiry)     // exp
                    .issuer(ISSUER)             // iss
                    .claim("role", rol)         // custom claim
                    .build();

            JWSObject jws = new JWSObject(header, new Payload(claims.toJSONObject()));
            jws.sign(new MACSigner(secretBytes()));

            return jws.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Error generando el token JWT", e);
        }
    }

    /** Valida el token y devuelve los claims si es válido; lanza excepción si no lo es. */
    public JWTClaimsSet validateAndGetClaims(String token) {
        try {
            JWSObject jws = JWSObject.parse(token);
            JWSVerifier verifier = new MACVerifier(secretBytes());

            if (!jws.verify(verifier)) {
                throw new InvalidTokenException("Firma del token inválida");
            }

            JWTClaimsSet claims = JWTClaimsSet.parse(jws.getPayload().toJSONObject());

            Date expiry = claims.getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new ExpiredTokenException("El token ha expirado");
            }

            return claims;
        } catch (ParseException | JOSEException e) {
            throw new InvalidTokenException("Token JWT malformado");
        }
    }
}