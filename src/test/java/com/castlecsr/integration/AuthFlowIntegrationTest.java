package com.castlecsr.integration;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // usa application-test.properties con H2
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String LOGIN_OK_BODY =
            "{\"username\":\"test_user\",\"password\":\"Password123!\"}";

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
    void flujoCompleto_loginSessionYEndpointsProtegidos() throws Exception {
        // 1. Login con credenciales válidas
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(LOGIN_OK_BODY))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("auth_token="));
        assertTrue(setCookieHeader.contains("HttpOnly"));

        // MockMvc no parsea el header "Cookie" a objetos Cookie; hay que pasarla explícitamente
        String token = setCookieHeader.split(";")[0].substring("auth_token=".length());

        // 2. Usar la cookie para consultar la sesión (endpoint protegido)
        mockMvc.perform(get("/api/auth/session")
                        .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test_user"))
                .andExpect(jsonPath("$.rol").value("USER"));

        // 3. Sin cookie, un endpoint protegido devuelve 401
        //    (/api/csr/historial aún no tiene controller — Fase 3 —, pero Spring Security
        //    lo bloquea antes de llegar al dispatcher, por eso el 401 aplica igual)
        mockMvc.perform(get("/api/csr/historial"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401YNoSeteaCookie() throws Exception {
        String loginBody = "{\"username\":\"test_user\",\"password\":\"password_incorrecto\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void login_conTokenExpirado_devuelve401EnEndpointProtegido() throws Exception {
        // Cookie con un token que no es un JWT válido: el filtro lo ignora → anónimo → 401
        mockMvc.perform(get("/api/auth/session")
                        .cookie(new Cookie("auth_token", "token-falso-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalidaLaCookieYBloqueaAccesoPosterior() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(LOGIN_OK_BODY))
                .andReturn();

        String token = loginResult.getResponse().getHeader("Set-Cookie")
                .split(";")[0].substring("auth_token=".length());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}