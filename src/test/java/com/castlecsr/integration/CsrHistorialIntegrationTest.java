package com.castlecsr.integration;

import com.castlecsr.model.CsrHistorial;
import com.castlecsr.model.Usuario;
import com.castlecsr.repository.CsrHistorialRepository;
import com.castlecsr.repository.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // usa application-test.properties con H2
class CsrHistorialIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CsrHistorialRepository csrHistorialRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario userA;
    private Usuario userB;
    private Long csrDeB;

    @BeforeEach
    void seed() {
        csrHistorialRepository.deleteAll();
        userA = usuarioRepository.findByUsername("hist_user_a")
                .orElseGet(() -> usuarioRepository.save(
                        new Usuario("hist_user_a", passwordEncoder.encode("Password123!"), "USER")));
        userB = usuarioRepository.findByUsername("hist_user_b")
                .orElseGet(() -> usuarioRepository.save(
                        new Usuario("hist_user_b", passwordEncoder.encode("Password123!"), "USER")));

        // 25 CSRs para el usuario A con fechas escalonadas
        for (int i = 0; i < 25; i++) {
            CsrHistorial record = new CsrHistorial(userA, "site" + i + ".example.com", "ACME Corp",
                    "MX", "Mexico City", "Mexico City", "RSA", "2048",
                    "-----BEGIN CERTIFICATE REQUEST-----...");
            record.setCreadoEn(LocalDateTime.now().minusMinutes(25 - i));
            csrHistorialRepository.save(record);
        }
        // 1 CSR del usuario B
        CsrHistorial deB = new CsrHistorial(userB, "otro.example.com", "Other Corp",
                "MX", "Mexico City", "Mexico City", "EC", "secp256r1",
                "-----BEGIN CERTIFICATE REQUEST-----...");
        csrDeB = csrHistorialRepository.save(deB).getId();
    }

    private Cookie loginComo(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = result.getResponse().getHeader("Set-Cookie")
                .split(";")[0].substring("auth_token=".length());
        return new Cookie("auth_token", token);
    }

    @Test
    void historial_paginado_devuelveSoloElPropioOrdenadoDesc() throws Exception {
        Cookie cookie = loginComo("hist_user_a");

        mockMvc.perform(get("/api/csr/historial?page=0&size=10").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                // el más reciente primero (site24 tiene la fecha más nueva)
                .andExpect(jsonPath("$.content[0].cn").value("site24.example.com"));

        mockMvc.perform(get("/api/csr/historial?page=2&size=10").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    @Test
    void historial_conBusquedaCaseInsensitive_filtra() throws Exception {
        Cookie cookie = loginComo("hist_user_a");

        mockMvc.perform(get("/api/csr/historial?search=SITE1").cookie(cookie))
                .andExpect(status().isOk())
                // site1, site10..site19 → 11 resultados
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    void detalles_deCsrPropio_devuelve200() throws Exception {
        Cookie cookie = loginComo("hist_user_b");

        mockMvc.perform(get("/api/csr/" + csrDeB).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cn").value("otro.example.com"))
                .andExpect(jsonPath("$.algorithm").value("EC-secp256r1"));
    }

    @Test
    void detalles_deCsrDeOtroUsuario_devuelve404() throws Exception {
        Cookie cookie = loginComo("hist_user_a");

        mockMvc.perform(get("/api/csr/" + csrDeB).cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void historial_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/csr/historial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historial_conSizeMayorA100_devuelve400() throws Exception {
        Cookie cookie = loginComo("hist_user_a");

        mockMvc.perform(get("/api/csr/historial?size=101").cookie(cookie))
                .andExpect(status().isBadRequest());
    }
}