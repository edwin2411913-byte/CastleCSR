package com.castlecsr.controller;

import com.castlecsr.exception.GlobalExceptionHandler;
import com.castlecsr.model.Usuario;
import com.castlecsr.security.CookieUtil;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.security.JwtTokenProvider;
import com.castlecsr.security.LoginRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CookieUtil cookieUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Rate limiter real con límites amplios: no interfiere con los tests existentes
        AuthController controller = new AuthController(
                authenticationManager, userDetailsService, jwtTokenProvider, cookieUtil,
                new LoginRateLimiter(100, 300));
        // Se registra GlobalExceptionHandler para probar el código de estado real
        // que produce BadCredentialsException (401) en el flujo completo
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_conCredencialesValidas_devuelve200YSeteaCookie() throws Exception {
        Usuario usuario = new Usuario("jgomez", "hash", "ADMIN");
        when(userDetailsService.loadUsuarioEntity("jgomez")).thenReturn(usuario);
        when(jwtTokenProvider.generateToken("jgomez", "ADMIN")).thenReturn("jwt-generado");
        when(cookieUtil.createAuthCookie("jwt-generado"))
                .thenReturn(ResponseCookie.from("auth_token", "jwt-generado").build());

        String body = "{\"username\":\"jgomez\",\"password\":\"pass123\"}";

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

        String body = "{\"username\":\"jgomez\",\"password\":\"malPassword\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void login_conBodyInvalido_devuelve400() throws Exception {
        String body = "{\"username\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
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
    }

    @Test
    void session_sinAutenticacion_devuelve401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conRateLimitSuperado_devuelve429ConRetryAfter() throws Exception {
        // Controller con límite de 2 intentos para provocar el bloqueo
        AuthController controller = new AuthController(
                authenticationManager, userDetailsService, jwtTokenProvider, cookieUtil,
                new LoginRateLimiter(2, 300));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        String body = "{\"username\":\"jgomez\",\"password\":\"malPassword\"}";

        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());

        // Tercer intento: la IP ya está bloqueada
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
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