# 🧩 Propuesta de Código — Fase 2: Autenticación JWT (Nimbus)

**Basado en:** `FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md`
**Decisión de integración:** el JWT se entrega en una **cookie HttpOnly** (`auth_token`), no en el body de la respuesta ni vía header `Authorization`. Esto permite que **`app.js` funcione sin ningún cambio**, ya que hoy en día:
- `fetch('/api/auth/login', ...)` no lee ningún token de la respuesta
- `fetch('/api/auth/session')` y `fetch('/api/csr/historial')` dependen de que el navegador reenvíe la sesión automáticamente (cosa que las cookies hacen por defecto en same-origin)

---

## 📂 Archivos nuevos y modificados

```
src/main/java/com/castlecsr/
├── config/
│   └── SecurityConfig.java                ← 🔧 MODIFICAR (agregar filtro JWT)
│
├── controller/
│   └── AuthController.java                ← 🆕 NUEVO
│
├── dto/
│   ├── LoginRequest.java                   ← 🆕 NUEVO
│   └── SessionResponse.java                ← ♻️ YA EXISTE (reutilizar)
│
├── exception/
│   ├── GlobalExceptionHandler.java         ← 🔧 MODIFICAR (nuevos handlers)
│   ├── InvalidTokenException.java          ← 🆕 NUEVO
│   └── ExpiredTokenException.java          ← 🆕 NUEVO
│
├── security/
│   ├── JwtTokenProvider.java               ← 🆕 NUEVO
│   ├── JwtAuthenticationFilter.java        ← 🆕 NUEVO
│   ├── CustomUserDetailsService.java       ← 🆕 NUEVO
│   ├── CookieUtil.java                     ← 🆕 NUEVO
│   └── JwtAuthenticationEntryPoint.java    ← 🆕 NUEVO
│
├── model/
│   └── Usuario.java                        ← ♻️ YA EXISTE (sin cambios)
│
└── repository/
    └── UsuarioRepository.java              ← ♻️ YA EXISTE (sin cambios)
```

---

## 1️⃣ `JwtTokenProvider.java` (nuevo)

Encapsula toda la lógica de Nimbus: generación y validación del token, usando los claims definidos en el plan (`sub`=username, `iat`, `exp`, `iss`, `role`).

```java
package com.castlecsr.security;

import com.nimbusds.jose.*;
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
                    .issueTime(now)              // iat
                    .expirationTime(expiry)      // exp
                    .issuer(ISSUER)              // iss
                    .claim("role", rol)          // custom claim
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
                throw new com.castlecsr.exception.InvalidTokenException("Firma del token inválida");
            }

            JWTClaimsSet claims = JWTClaimsSet.parse(jws.getPayload().toJSONObject());

            Date expiry = claims.getExpirationTime();
            if (expiry == null || expiry.before(new Date())) {
                throw new com.castlecsr.exception.ExpiredTokenException("El token ha expirado");
            }

            return claims;
        } catch (ParseException | JOSEException e) {
            throw new com.castlecsr.exception.InvalidTokenException("Token JWT malformado");
        }
    }
}
```

**Configuración asociada (`application-local.properties` — modificar):**
```properties
jwt.secret=${JWT_SECRET:default-secret}
jwt.expiration-ms=1800000
```

---

## 2️⃣ `CookieUtil.java` (nuevo)

Centraliza la creación/lectura/borrado de la cookie `auth_token`, para no repetir esta lógica en el controller y en el filtro.

```java
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

    // 🆕 Configurable por perfil: false en local (sin HTTPS), true en prod
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
                .sameSite("Strict") // 🆕 mitiga CSRF: la cookie no se envía en requests cross-site
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
```

**Configuración por perfil:**

`application-local.properties` (desarrollo, sin HTTPS):
```properties
jwt.cookie-secure=false
```

`application-prod.properties` (producción, con HTTPS):
```properties
jwt.cookie-secure=true
```

### 🔐 ¿Por qué `SameSite=Strict` y no `Lax`?

| Valor | Comportamiento | ¿Cuándo usarlo? |
|---|---|---|
| `Strict` | La cookie **nunca** se envía en navegación cross-site, ni siquiera al hacer clic en un link externo hacia tu app | Ideal cuando frontend y backend son same-origin, como en CastleCSR |
| `Lax` | Se envía en navegación top-level (ej. clic en un link), pero no en requests de fondo (imágenes, fetch) desde otro sitio | Útil si tu app necesita recibir tráfico desde links externos (ej. un email con "ir a mi cuenta") |

Como tu frontend y backend viven en el mismo origen (`localhost:8080` sirviendo todo), `Strict` no genera fricción y cierra el vector de CSRF casi por completo.

⚠️ **Importante:** `Secure=false` y `SameSite=Strict` combinados están bien en local, pero antes de producción ambos deben revisarse: `Secure` pasa a `true` (vía la property), y `SameSite=Strict` se mantiene igual.

---

## 3️⃣ `CustomUserDetailsService.java` (nuevo)

Usa el `UsuarioRepository` **ya existente** — sin modificarlo — para cargar el usuario y construir el `UserDetails` que Spring Security necesita.

```java
package com.castlecsr.security;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new User(
                usuario.getUsername(),
                usuario.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }

    /** Utilidad para que AuthController obtenga la entidad completa (rol incluido) tras validar credenciales. */
    public Usuario loadUsuarioEntity(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
}
```

---

## 4️⃣ `JwtAuthenticationFilter.java` (nuevo)

Se ejecuta en cada request: lee el token de la **cookie** (no del header), lo valida con `JwtTokenProvider` y, si es válido, autentica al usuario en el `SecurityContext`.

```java
package com.castlecsr.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CookieUtil cookieUtil) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = cookieUtil.extractToken(request);

        if (token != null) {
            try {
                JWTClaimsSet claims = jwtTokenProvider.validateAndGetClaims(token);
                String username = claims.getSubject();
                String role = claims.getStringClaim("role");

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                // Token inválido o expirado: no se autentica; el request sigue como anónimo
                // y SecurityConfig decide si el endpoint requiere autenticación (→ 401 vía entry point)
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 5️⃣ `JwtAuthenticationEntryPoint.java` (nuevo)

Da formato JSON consistente a los 401, igual al que ya usa `GlobalExceptionHandler` para el resto de errores (`{"status":401,"error":"Unauthorized","message":"..."}`).

```java
package com.castlecsr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.castlecsr.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse error = new ErrorResponse(401, "Unauthorized", "Autenticación requerida");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
```

---

## 6️⃣ `LoginRequest.java` (nuevo DTO)

```java
package com.castlecsr.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    public LoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

---

## 7️⃣ `AuthController.java` (nuevo)

Implementa los 3 endpoints que **`app.js` ya está esperando**: `POST /api/auth/login`, `GET /api/auth/session`, `POST /api/auth/logout`.

```java
package com.castlecsr.controller;

import com.castlecsr.dto.LoginRequest;
import com.castlecsr.dto.SessionResponse;
import com.castlecsr.model.Usuario;
import com.castlecsr.security.CookieUtil;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    public AuthController(AuthenticationManager authenticationManager,
                           CustomUserDetailsService userDetailsService,
                           JwtTokenProvider jwtTokenProvider,
                           CookieUtil cookieUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Usuario usuario = userDetailsService.loadUsuarioEntity(request.getUsername());
        String token = jwtTokenProvider.generateToken(usuario.getUsername(), usuario.getRol());

        ResponseCookie cookie = cookieUtil.createAuthCookie(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        Usuario usuario = userDetailsService.loadUsuarioEntity(username);

        return ResponseEntity.ok(new SessionResponse(usuario.getId(), usuario.getUsername(), usuario.getRol()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = cookieUtil.createExpiredCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
```

⚠️ **Nota sobre `BadCredentialsException`:** ya es una excepción estándar de Spring Security. En el `GlobalExceptionHandler` (modificación abajo) se mapea a 401 con mensaje genérico, siguiendo la buena práctica de no revelar si falló el username o el password.

---

## 8️⃣ `GlobalExceptionHandler.java` (modificar — agregar handlers)

Se agregan estos métodos al handler **ya existente**, sin tocar los que ya maneja hoy:

```java
// Agregar estos imports:
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import com.castlecsr.exception.InvalidTokenException;
import com.castlecsr.exception.ExpiredTokenException;

// Agregar estos métodos dentro de la clase existente GlobalExceptionHandler:

@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
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
```

**Excepciones nuevas y simples que las respaldan:**

```java
package com.castlecsr.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}
```

```java
package com.castlecsr.exception;

public class ExpiredTokenException extends RuntimeException {
    public ExpiredTokenException(String message) { super(message); }
}
```

---

## 9️⃣ `SecurityConfig.java` (modificar)

Este es el cambio más delicado porque **ya existe** y define qué es público. Se muestra completo para que sea fácil compararlo con el actual y ver exactamente qué se agrega (marcado con `// 🆕`).

```java
package com.castlecsr.config;

import com.castlecsr.security.CookieUtil;
import com.castlecsr.security.JwtAuthenticationEntryPoint;
import com.castlecsr.security.JwtAuthenticationFilter;
import com.castlecsr.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 🆕 Nuevas dependencias para JWT
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                           CookieUtil cookieUtil,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieUtil = cookieUtil;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // ← YA EXISTÍA, sin cambios
    }

    // 🆕 Necesario para que AuthController pueda llamar authenticationManager.authenticate(...)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider, cookieUtil); // 🆕

        http
            .csrf(csrf -> csrf.disable()) // ← YA EXISTÍA (API REST)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ← YA EXISTÍA
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 🆕 sin sesiones de servidor
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)) // 🆕 401 con formato JSON
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/health", "/api/info").permitAll()   // ← YA EXISTÍA
                    .requestMatchers("/api/auth/login").permitAll()            // 🆕
                    .anyRequest().authenticated()                              // ← YA EXISTÍA
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // 🆕

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // ← YA EXISTÍA, se muestra igual para referencia
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 🆕 IMPRESCINDIBLE: permite que el navegador envíe/reciba cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

⚠️ **El cambio más crítico de este archivo:** `config.setAllowCredentials(true)` en CORS. Sin esto, aunque todo lo demás esté bien, el navegador **no enviará ni aceptará la cookie** en requests cross-origin. Si frontend y backend siguen sirviéndose desde el mismo origen (`localhost:8080`), esto es menos crítico, pero es buena práctica dejarlo explícito desde ahora.

---

## 🔗 Resumen de integración con el código existente

| Componente existente | ¿Se modifica? | Cómo |
|---|---|---|
| `Usuario.java` | ❌ No | Se usa tal cual (`getId()`, `getUsername()`, `getPasswordHash()`, `getRol()`) — **confirmado con código real** |
| `UsuarioRepository.java` | ❌ No | Ya tiene `findByUsername(String)` y `existsByUsername(String)` — **confirmado con código real** |
| `SecurityConfig.java` | ✅ Sí | Se agrega el filtro JWT, el entry point 401, política stateless y `allowCredentials(true)` |
| `GlobalExceptionHandler.java` | ✅ Sí | Se agregan 4 `@ExceptionHandler` nuevos, sin tocar los existentes |
| `ErrorResponse.java` | ❌ No | Se reutiliza tal cual — constructor `(status, error, message)` **confirmado con código real** |
| `SessionResponse.java` | ❌ No | Se reutiliza; constructor real es `(Long id, String username, String rol)` — `AuthController` ya ajustado a esto |
| `app.js` / `login.html` | ❌ No | Cero cambios — la cookie HttpOnly viaja automáticamente |

---

## 🧪 Cómo probar manualmente

```bash
# Login (guarda la cookie con -c)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jgomez","password":"tu_password"}'

# Ver sesión (reenvía la cookie con -b)
curl -i -b cookies.txt http://localhost:8080/api/auth/session

# Logout
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/logout

# Confirmar que /api/csr/historial ahora exige sesión
curl -i http://localhost:8080/api/csr/historial
# Esperado: 401 Unauthorized
```

---

## 📋 Pendientes a confirmar contigo antes de implementar

- [x] ~~Confirmar que `UsuarioRepository` ya tiene `findByUsername(String)`~~ → ✅ Confirmado, ya existe
- [x] ~~Confirmar el constructor exacto de `ErrorResponse` y `SessionResponse`~~ → ✅ Confirmado, código ya ajustado a las clases reales
- [x] ~~Decidir el valor de `jwt.cookie-secure` para el perfil `local` (HTTP) vs `prod` (HTTPS)~~ → ✅ Resuelto: `false` en `local` (sin HTTPS por ahora), `true` en `prod`
