package com.castlecsr.controller;

import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.exception.CsrGenerationException;
import com.castlecsr.exception.GlobalExceptionHandler;
import com.castlecsr.model.Usuario;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.service.CsrService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CsrControllerTest {

    @Mock private CsrService csrService;
    @Mock private CustomUserDetailsService userDetailsService;

    private MockMvc mockMvc;

    private static final String BODY_VALIDO = """
            {
              "cn": "example.com",
              "o": "ACME Corp",
              "ou": "IT",
              "c": "MX",
              "st": "Mexico City",
              "l": "Mexico City",
              "sans": ["DNS:example.com"],
              "keyType": "RSA",
              "keySize": 2048,
              "password": "MiContraseña123",
              "passwordConfirm": "MiContraseña123"
            }
            """;

    @BeforeEach
    void setUp() {
        CsrController controller = new CsrController(csrService, userDetailsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void generarCsr_conRequestValido_devuelve200ConPems() throws Exception {
        autenticarComo("test_user");

        Usuario usuario = new Usuario("test_user", "hash", "USER");
        usuario.setId(1L);
        when(userDetailsService.loadUsuarioEntity("test_user")).thenReturn(usuario);
        when(csrService.generateCsr(any(), any())).thenReturn(
                new CsrGenerationResponse("42", "-----BEGIN CERTIFICATE REQUEST-----...",
                        "-----BEGIN ENCRYPTED PRIVATE KEY-----..."));

        mockMvc.perform(post("/api/csr/generar")
                        .contentType("application/json")
                        .content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrId").value("42"))
                .andExpect(jsonPath("$.csr").exists())
                .andExpect(jsonPath("$.keyEncrypted").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void generarCsr_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/csr/generar")
                        .contentType("application/json")
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generarCsr_conBodyInvalido_devuelve400ConErroresDeValidacion() throws Exception {
        autenticarComo("test_user");

        String bodyInvalido = "{\"cn\":\"\",\"keyType\":\"DSA\",\"password\":\"corta\"}";

        mockMvc.perform(post("/api/csr/generar")
                        .contentType("application/json")
                        .content(bodyInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void generarCsr_conErrorDeGeneracion_devuelve400ConMensaje() throws Exception {
        autenticarComo("test_user");

        Usuario usuario = new Usuario("test_user", "hash", "USER");
        when(userDetailsService.loadUsuarioEntity("test_user")).thenReturn(usuario);
        when(csrService.generateCsr(any(), any()))
                .thenThrow(new CsrGenerationException("Las contraseñas no coinciden"));

        mockMvc.perform(post("/api/csr/generar")
                        .contentType("application/json")
                        .content(BODY_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden"));
    }
}