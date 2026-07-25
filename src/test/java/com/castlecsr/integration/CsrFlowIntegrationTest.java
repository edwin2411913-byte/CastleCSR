package com.castlecsr.integration;

import com.castlecsr.model.Usuario;
import com.castlecsr.repository.CsrHistorialRepository;
import com.castlecsr.repository.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // usa application-test.properties con H2
class CsrFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CsrHistorialRepository csrHistorialRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String LOGIN_BODY =
            "{\"username\":\"csr_user\",\"password\":\"Password123!\"}";

    private static final String CSR_RSA_BODY = """
            {
              "cn": "example.com",
              "o": "ACME Corp",
              "ou": "IT",
              "c": "MX",
              "st": "Mexico City",
              "l": "Mexico City",
              "sans": ["DNS:example.com", "DNS:www.example.com"],
              "keyType": "RSA",
              "keySize": 2048,
              "password": "MiContraseña123",
              "passwordConfirm": "MiContraseña123"
            }
            """;

    @BeforeEach
    void seedUsuario() {
        if (usuarioRepository.findByUsername("csr_user").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "csr_user", passwordEncoder.encode("Password123!"), "USER"));
        }
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getHeader("Set-Cookie")
                .split(";")[0].substring("auth_token=".length());
    }

    @Test
    void flujoCompleto_loginGenerarCsrRsaYPersistir() throws Exception {
        String token = login();
        long antes = csrHistorialRepository.count();

        MvcResult result = mockMvc.perform(post("/api/csr/generar")
                        .cookie(new Cookie("auth_token", token))
                        .contentType("application/json")
                        .content(CSR_RSA_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csrId").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String csrPem = json.get("csr").asText();
        String keyPem = json.get("keyEncrypted").asText();

        // El CSR es un PKCS#10 parseable con el subject correcto
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(pemToDer(csrPem));
        assertTrue(csr.getSubject().toString().contains("CN=example.com"));

        // La clave privada es un EncryptedPrivateKeyInfo válido (cifrada)
        PKCS8EncryptedPrivateKeyInfo encKey = new PKCS8EncryptedPrivateKeyInfo(pemToDer(keyPem));
        assertNotNull(encKey.getEncryptionAlgorithm());

        // Se persistió un registro sin la clave privada
        assertEquals(antes + 1, csrHistorialRepository.count());
    }

    @Test
    void generarCsrEc_devuelveCsrValido() throws Exception {
        String token = login();

        String body = CSR_RSA_BODY
                .replace("\"keyType\": \"RSA\"", "\"keyType\": \"EC\"")
                .replace("\"keySize\": 2048", "\"curve\": \"secp256r1\"");

        mockMvc.perform(post("/api/csr/generar")
                        .cookie(new Cookie("auth_token", token))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csr").exists())
                .andExpect(jsonPath("$.keyEncrypted").exists());
    }

    @Test
    void generarCsr_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(post("/api/csr/generar")
                        .contentType("application/json")
                        .content(CSR_RSA_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generarCsr_conContraseniasDistintas_devuelve400() throws Exception {
        String token = login();

        String body = CSR_RSA_BODY.replace(
                "\"passwordConfirm\": \"MiContraseña123\"",
                "\"passwordConfirm\": \"OtraPassword999\"");

        mockMvc.perform(post("/api/csr/generar")
                        .cookie(new Cookie("auth_token", token))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden"));
    }

    private static byte[] pemToDer(String pem) {
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}