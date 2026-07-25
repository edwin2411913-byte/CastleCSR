package com.castlecsr.controller;

import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.dto.CsrHistorialResponse;
import com.castlecsr.exception.CsrGenerationException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    // ---- Fase 4: historial ----

    private Usuario usuarioAutenticado() {
        autenticarComo("test_user");
        Usuario usuario = new Usuario("test_user", "hash", "USER");
        usuario.setId(7L);
        when(userDetailsService.loadUsuarioEntity("test_user")).thenReturn(usuario);
        return usuario;
    }

    private CsrHistorialResponse dtoHistorial(Long id, String cn) {
        return new CsrHistorialResponse(id, cn, "ACME Corp", "RSA-2048", LocalDateTime.now());
    }

    @Test
    void historial_conAutenticacion_devuelve200Paginado() throws Exception {
        usuarioAutenticado();
        when(csrService.getHistorial(eq(7L), eq(0), eq(20), any()))
                .thenReturn(new PageImpl<>(List.of(dtoHistorial(1L, "example.com")),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/csr/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].cn").value("example.com"))
                .andExpect(jsonPath("$.content[0].algorithm").value("RSA-2048"));
    }

    @Test
    void historial_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/csr/historial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historial_conBusqueda_pasaParametrosAlServicio() throws Exception {
        usuarioAutenticado();
        when(csrService.getHistorial(7L, 0, 5, "example"))
                .thenReturn(new PageImpl<>(List.of(dtoHistorial(1L, "example.com")),
                        PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/csr/historial")
                        .param("page", "0").param("size", "5").param("search", "example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cn").value("example.com"));
    }

    @Test
    void historial_conSizeInvalido_devuelve400() throws Exception {
        usuarioAutenticado();
        when(csrService.getHistorial(7L, 0, 500, null))
                .thenThrow(new IllegalArgumentException("size debe estar entre 1 y 100"));

        mockMvc.perform(get("/api/csr/historial").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalles_propietario_devuelve200() throws Exception {
        usuarioAutenticado();
        when(csrService.getCsrDetails(5L, 7L)).thenReturn(dtoHistorial(5L, "detail.com"));

        mockMvc.perform(get("/api/csr/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.cn").value("detail.com"));
    }

    @Test
    void detalles_noPropietario_devuelve404() throws Exception {
        usuarioAutenticado();
        when(csrService.getCsrDetails(99L, 7L))
                .thenThrow(new EntityNotFoundException("CSR no encontrado"));

        mockMvc.perform(get("/api/csr/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void detalles_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/csr/5"))
                .andExpect(status().isUnauthorized());
    }
}