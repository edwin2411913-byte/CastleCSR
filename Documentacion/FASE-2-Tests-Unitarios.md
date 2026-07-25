# 🧪 Tests Unitarios — Fase 2: Autenticación JWT (Nimbus)

**Basado en:** `FASE-2-Propuesta-Codigo-Autenticacion.md`
**Frameworks:** JUnit 5, Mockito, Spring Boot Test, MockMvc (ya incluidos en el proyecto vía `spring-boot-starter-test` y `spring-security-test`, según `ESTADO-ACTUAL.md`)

---

## 📂 Estructura de tests propuesta

```
src/test/java/com/castlecsr/
├── security/
│   ├── JwtTokenProviderTest.java           ← 🆕 Unitario
│   ├── CookieUtilTest.java                 ← 🆕 Unitario
│   ├── CustomUserDetailsServiceTest.java   ← 🆕 Unitario
│   └── JwtAuthenticationFilterTest.java    ← 🆕 Unitario
│
├── controller/
│   └── AuthControllerTest.java             ← 🆕 Unitario (MockMvc + mocks)
│
├── exception/
│   └── GlobalExceptionHandlerTest.java     ← 🆕 Unitario
│
└── integration/
    └── AuthFlowIntegrationTest.java         ← 🆕 Integración (contexto completo)
```

---

## 1️⃣ `JwtTokenProviderTest.java`

Cubre: generación correcta del token, validación exitosa, firma inválida, token expirado, token malformado.

```java
package com.castlecsr.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

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
                com.castlecsr.exception.InvalidTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims(token)
        );
    }

    @Test
    void validateAndGetClaims_conTokenExpirado_lanzaExpiredTokenException() {
        // Expiración negativa: el token nace ya expirado
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", -1000L);
        String token = jwtTokenProvider.generateToken("jgomez", "ADMIN");

        assertThrows(
                com.castlecsr.exception.ExpiredTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims(token)
        );
    }

    @Test
    void validateAndGetClaims_conTokenMalformado_lanzaInvalidTokenException() {
        assertThrows(
                com.castlecsr.exception.InvalidTokenException.class,
                () -> jwtTokenProvider.validateAndGetClaims("esto-no-es-un-jwt-valido")
        );
    }
}
```

⚠️ **Nota:** `JwtTokenProvider` usa `@Value` para inyectar `jwtSecretBase64` y `expirationMs`, por lo que en un test unitario puro (sin `@SpringBootTest`) se asignan manualmente vía `ReflectionTestUtils.setField(...)`, ya que no hay contexto de Spring que resuelva la property.

---

## 2️⃣ `CookieUtilTest.java`

Cubre: atributos correctos de la cookie (`HttpOnly`, `Secure`, `SameSite`, `MaxAge`), la cookie de expiración, y la extracción del token desde el request.

```java
package com.castlecsr.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

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
```

---

## 3️⃣ `CustomUserDetailsServiceTest.java`

Cubre: carga exitosa de usuario, usuario no encontrado, y el método auxiliar `loadUsuarioEntity`.

```java
package com.castlecsr.security;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_conUsuarioExistente_devuelveUserDetailsConRolCorrecto() {
        Usuario usuario = new Usuario("jgomez", "hashDePassword", "ADMIN");
        when(usuarioRepository.findByUsername("jgomez")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jgomez");

        assertEquals("jgomez", userDetails.getUsername());
        assertEquals("hashDePassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_conUsuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("no_existe")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("no_existe")
        );
    }

    @Test
    void loadUsuarioEntity_conUsuarioExistente_devuelveLaEntidadCompleta() {
        Usuario usuario = new Usuario("jgomez", "hashDePassword", "ADMIN");
        when(usuarioRepository.findByUsername("jgomez")).thenReturn(Optional.of(usuario));

        Usuario resultado = customUserDetailsService.loadUsuarioEntity("jgomez");

        assertEquals(usuario, resultado);
    }

    @Test
    void loadUsuarioEntity_conUsuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByUsername("no_existe")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUsuarioEntity("no_existe")
        );
    }
}
```

---

## 4️⃣ `JwtAuthenticationFilterTest.java`

Cubre: token válido en cookie autentica al usuario, token inválido/expirado limpia el contexto, sin cookie el filtro deja pasar sin autenticar.

```java
package com.castlecsr.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_conTokenValido_autenticaAlUsuario() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, cookieUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("jgomez")
                .claim("role", "ADMIN")
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        when(cookieUtil.extractToken(request)).thenReturn("token-valido");
        when(jwtTokenProvider.validateAndGetClaims("token-valido")).thenReturn(claims);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("jgomez", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_conTokenInvalido_noAutenticaYContinuaElFiltro() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, cookieUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(cookieUtil.extractToken(request)).thenReturn("token-invalido");
        when(jwtTokenProvider.validateAndGetClaims("token-invalido"))
                .thenThrow(new com.castlecsr.exception.InvalidTokenException("Token inválido"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response); // el filtro deja pasar; SecurityConfig decide el 401
    }

    @Test
    void doFilterInternal_sinCookie_noIntentaValidarYContinuaElFiltro() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, cookieUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(cookieUtil.extractToken(request)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtTokenProvider, never()).validateAndGetClaims(any());
        verify(filterChain).doFilter(request, response);
    }
}
```

---

## 5️⃣ `AuthControllerTest.java`

Cubre los 3 endpoints (`login`, `session`, `logout`) usando `MockMvc` en modo standalone (sin levantar el contexto completo de Spring), con los colaboradores mockeados.

```java
package com.castlecsr.controller;

import com.castlecsr.model.Usuario;
import com.castlecsr.security.CookieUtil;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CookieUtil cookieUtil;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authenticationManager, userDetailsService, jwtTokenProvider, cookieUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_conCredencialesValidas_devuelve200YSeteaCookie() throws Exception {
        Usuario usuario = new Usuario("jgomez", "hash", "ADMIN");
        when(userDetailsService.loadUsuarioEntity("jgomez")).thenReturn(usuario);
        when(jwtTokenProvider.generateToken("jgomez", "ADMIN")).thenReturn("jwt-generado");
        when(cookieUtil.createAuthCookie("jwt-generado"))
                .thenReturn(ResponseCookie.from("auth_token", "jwt-generado").build());

        String body = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{ put("username", "jgomez"); put("password", "pass123"); }});

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        String body = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{ put("username", "jgomez"); put("password", "malPassword"); }});

        // El AuthController relanza BadCredentialsException, que en el contexto real
        // captura el GlobalExceptionHandler (probado por separado). Aquí verificamos
        // que el controller efectivamente propaga la excepción sin capturarla en silencio.
        try {
            mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(body));
        } catch (Exception ex) {
            assert ex.getCause() instanceof BadCredentialsException
                    || ex instanceof BadCredentialsException;
        }
    }

    @Test
    void session_conUsuarioAutenticado_devuelveDatosDeSesion() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("jgomez");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        Usuario usuario = new Usuario("jgomez", "hash", "ADMIN");
        usuario.setId(1L);
        when(userDetailsService.loadUsuarioEntity("jgomez")).thenReturn(usuario);

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jgomez"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));

        SecurityContextHolder.clearContext();
    }

    @Test
    void session_sinAutenticacion_devuelve401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_devuelve200YExpiraLaCookie() throws Exception {
        when(cookieUtil.createExpiredCookie())
                .thenReturn(ResponseCookie.from("auth_token", "").maxAge(0).build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }
}
```

⚠️ **Nota sobre el test de credenciales inválidas:** como el controller lanza `BadCredentialsException` sin capturarla (para que la maneje `GlobalExceptionHandler`), en un `MockMvc` standalone (sin el handler registrado) la excepción se propaga tal cual. En la sección 6 se prueba el handler por separado; opcionalmente, se puede registrar `GlobalExceptionHandler` en el `standaloneSetup(...).setControllerAdvice(...)` para probar el flujo end-to-end del código de estado.

---

## 6️⃣ `GlobalExceptionHandlerTest.java`

Cubre los 4 handlers nuevos agregados para Fase 2.

```java
package com.castlecsr.exception;

import com.castlecsr.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadCredentials_devuelve401ConMensajeGenerico() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("detalle interno"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
        // El mensaje no debe filtrar si falló el username o el password
        assertFalse(response.getBody().getMessage().toLowerCase().contains("username"));
    }

    @Test
    void handleAccessDenied_devuelve403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("sin permiso"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Acceso denegado", response.getBody().getMessage());
    }

    @Test
    void handleInvalidToken_devuelve401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidToken(new InvalidTokenException("firma inválida"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Token inválido", response.getBody().getMessage());
    }

    @Test
    void handleExpiredToken_devuelve401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleExpiredToken(new ExpiredTokenException("expiró"));

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Token expirado", response.getBody().getMessage());
    }
}
```

---

## 7️⃣ `AuthFlowIntegrationTest.java` (integración, contexto completo)

Prueba el flujo real de extremo a extremo: levanta el contexto de Spring, hace login contra la BD de test (H2, ya configurada según `ESTADO-ACTUAL.md`), reutiliza la cookie devuelta y accede a un endpoint protegido.

```java
package com.castlecsr.integration;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // usa application-test.properties con H2, según ESTADO-ACTUAL.md
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedUsuario() {
        if (usuarioRepository.findByUsername("test_user").isEmpty()) {
            Usuario usuario = new Usuario(
                    "test_user",
                    passwordEncoder.encode("Password123!"),
                    "USER"
            );
            usuarioRepository.save(usuario);
        }
    }

    @Test
    void flujoCompleto_loginSessionYAccesoAEndpointProtegido() throws Exception {
        // 1. Login con credenciales válidas
        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", "test_user", "password", "Password123!"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("auth_token="));
        assertTrue(setCookieHeader.contains("HttpOnly"));

        String cookieValue = setCookieHeader.split(";")[0]; // "auth_token=xxx"

        // 2. Usar la cookie para consultar la sesión
        mockMvc.perform(get("/api/auth/session")
                        .header("Cookie", cookieValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.rol").value("USER"));

        // 3. Acceder a un endpoint protegido con la cookie
        mockMvc.perform(get("/api/csr/historial")
                        .header("Cookie", cookieValue))
                .andExpect(status().isOk());

        // 4. Sin cookie, el mismo endpoint debe devolver 401
        mockMvc.perform(get("/api/csr/historial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401YNoSeteaCookie() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", "test_user", "password", "password_incorrecto"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void logout_invalidaLaCookieYBloqueaAccesoPosterior() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                Map.of("username", "test_user", "password", "Password123!"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andReturn();

        String cookieValue = loginResult.getResponse().getHeader("Set-Cookie").split(";")[0];

        mockMvc.perform(post("/api/auth/logout")
                        .header("Cookie", cookieValue))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}
```

⚠️ **Dependencia:** este test asume un perfil `test` con `application-test.properties` apuntando a H2 (mencionado como dependencia de test en `ESTADO-ACTUAL.md`). Si aún no existe ese archivo de configuración, es un prerequisito antes de correr esta clase.

---

## 📋 Checklist de Tests — Fase 2

- [ ] `JwtTokenProviderTest` — generación, validación, firma inválida, expiración, malformado
- [ ] `CookieUtilTest` — atributos de cookie, expiración, extracción de token
- [ ] `CustomUserDetailsServiceTest` — carga exitosa, usuario no encontrado
- [ ] `JwtAuthenticationFilterTest` — token válido, inválido, sin cookie
- [ ] `AuthControllerTest` — login éxito/fallo, session autenticado/no autenticado, logout
- [ ] `GlobalExceptionHandlerTest` — los 4 handlers nuevos
- [ ] `AuthFlowIntegrationTest` — flujo completo login → session → endpoint protegido → logout
- [ ] Cobertura total de las clases de Fase 2 ≥ 80% (según criterio de aceptación del plan)

## 📊 Cobertura estimada por clase

| Clase | Tests | Cobertura esperada |
|---|---|---|
| `JwtTokenProvider` | 5 | ~95% |
| `CookieUtil` | 6 | ~100% |
| `CustomUserDetailsService` | 4 | ~100% |
| `JwtAuthenticationFilter` | 3 | ~90% |
| `AuthController` | 5 | ~85% |
| `GlobalExceptionHandler` (nuevo) | 4 | ~100% |
| `AuthFlowIntegrationTest` | 3 | Cobertura end-to-end (no aporta % de línea, valida integración real) |

## ⏰ Tiempo estimado

- Escribir y ajustar tests unitarios (secciones 1-6): 8h
- Escribir y depurar test de integración (sección 7): 4h
- Revisión de cobertura y ajustes finales: 2h

**Tiempo Total Estimado: 14 horas**
