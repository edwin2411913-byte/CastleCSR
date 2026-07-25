package com.castlecsr.service;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.dto.CsrHistorialResponse;
import com.castlecsr.exception.CsrGenerationException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.castlecsr.model.CsrHistorial;
import com.castlecsr.model.Usuario;
import com.castlecsr.repository.CsrHistorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsrServiceTest {

    @Mock private CsrHistorialRepository csrHistorialRepository;

    private CsrService csrService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // CryptographyService real: la criptografía EC es rápida para tests
        csrService = new CsrService(new CryptographyService(), csrHistorialRepository);
        usuario = new Usuario("test_user", "hash", "USER");
        usuario.setId(7L);
    }

    private CsrGenerationRequest requestValido() {
        CsrGenerationRequest req = new CsrGenerationRequest();
        req.setCn("example.com");
        req.setO("ACME Corp");
        req.setOu("IT");
        req.setC("MX");
        req.setSt("Mexico City");
        req.setL("Mexico City");
        req.setSans(List.of("DNS:example.com", "DNS:www.example.com"));
        req.setKeyType("EC");
        req.setCurve("secp256r1");
        req.setPassword("MiContraseña123");
        req.setPasswordConfirm("MiContraseña123");
        return req;
    }

    @Test
    void generateCsr_conRequestValido_devuelvePemsYPersiste() {
        when(csrHistorialRepository.save(any(CsrHistorial.class))).thenAnswer(inv -> {
            CsrHistorial record = inv.getArgument(0);
            record.setId(42L);
            return record;
        });

        CsrGenerationResponse response = csrService.generateCsr(requestValido(), usuario);

        assertEquals("42", response.getCsrId());
        assertTrue(response.getCsr().startsWith("-----BEGIN CERTIFICATE REQUEST-----"));
        assertTrue(response.getKeyEncrypted().startsWith("-----BEGIN ENCRYPTED PRIVATE KEY-----"));

        ArgumentCaptor<CsrHistorial> captor = ArgumentCaptor.forClass(CsrHistorial.class);
        verify(csrHistorialRepository).save(captor.capture());
        CsrHistorial saved = captor.getValue();
        assertEquals("example.com", saved.getCommonName());
        assertEquals("EC", saved.getAlgoritmo());
        assertEquals("secp256r1", saved.getTamanioOCurva());
        assertEquals("DNS:example.com,DNS:www.example.com", saved.getSan());
        // La clave privada nunca se guarda: solo el CSR público
        assertEquals(response.getCsr(), saved.getCsrPem());
        assertFalse(saved.getCsrPem().contains("PRIVATE KEY"));
    }

    @Test
    void generateCsr_conContraseniasQueNoCoinciden_lanzaExcepcion() {
        CsrGenerationRequest req = requestValido();
        req.setPasswordConfirm("Otra_password1");

        CsrGenerationException ex = assertThrows(CsrGenerationException.class,
                () -> csrService.generateCsr(req, usuario));
        assertTrue(ex.getMessage().contains("contraseñas"));
        verify(csrHistorialRepository, never()).save(any());
    }

    @Test
    void generateCsr_conCountryInvalido_lanzaExcepcion() {
        CsrGenerationRequest req = requestValido();
        req.setC("XX");

        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));
    }

    @Test
    void generateCsr_conSanInvalido_lanzaExcepcion() {
        CsrGenerationRequest req = requestValido();
        req.setSans(List.of("EMAIL:foo@bar.com"));
        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));

        req.setSans(List.of("IP:999.999.999.999.999"));
        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));

        req.setSans(List.of("DNS:dominio con espacios"));
        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));
    }

    @Test
    void generateCsr_rsaSinKeySize_lanzaExcepcion() {
        CsrGenerationRequest req = requestValido();
        req.setKeyType("RSA");
        req.setKeySize(null);

        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));
    }

    @Test
    void generateCsr_ecSinCurve_lanzaExcepcion() {
        CsrGenerationRequest req = requestValido();
        req.setCurve(null);

        assertThrows(CsrGenerationException.class, () -> csrService.generateCsr(req, usuario));
    }

    @Test
    void generateCsr_conSanIp_esValido() {
        when(csrHistorialRepository.save(any(CsrHistorial.class))).thenAnswer(inv -> {
            CsrHistorial record = inv.getArgument(0);
            record.setId(1L);
            return record;
        });

        CsrGenerationRequest req = requestValido();
        req.setSans(List.of("DNS:example.com", "IP:192.168.1.1"));

        CsrGenerationResponse response = csrService.generateCsr(req, usuario);
        assertNotNull(response.getCsr());
    }

    // ---- Fase 4: historial ----

    private CsrHistorial historialRecord(Long id, String cn) {
        CsrHistorial record = new CsrHistorial(usuario, cn, "ACME Corp",
                "MX", "Mexico City", "Mexico City", "RSA", "2048", "-----BEGIN CERTIFICATE REQUEST-----...");
        record.setId(id);
        return record;
    }

    @Test
    void getHistorial_sinBusqueda_devuelvePaginaConvertida() {
        when(csrHistorialRepository.findByUsuarioIdOrderByCreadoEnDesc(eq(7L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(historialRecord(1L, "example.com"))));

        Page<CsrHistorialResponse> page = csrService.getHistorial(7L, 0, 20, null);

        assertEquals(1, page.getTotalElements());
        CsrHistorialResponse dto = page.getContent().get(0);
        assertEquals(1L, dto.getId());
        assertEquals("example.com", dto.getCn());
        assertEquals("ACME Corp", dto.getOrganization());
        assertEquals("RSA-2048", dto.getAlgorithm());
        assertNotNull(dto.getFechaGeneracion());
    }

    @Test
    void getHistorial_conBusqueda_usaMetodoCaseInsensitive() {
        when(csrHistorialRepository
                .findByUsuarioIdAndCommonNameContainingIgnoreCaseOrderByCreadoEnDesc(
                        eq(7L), eq("Example"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(historialRecord(2L, "example.com"))));

        Page<CsrHistorialResponse> page = csrService.getHistorial(7L, 0, 20, "Example");

        assertEquals(1, page.getTotalElements());
        verify(csrHistorialRepository)
                .findByUsuarioIdAndCommonNameContainingIgnoreCaseOrderByCreadoEnDesc(
                        eq(7L), eq("Example"), any(Pageable.class));
    }

    @Test
    void getHistorial_conPaginacionInvalida_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> csrService.getHistorial(7L, -1, 20, null));
        assertThrows(IllegalArgumentException.class, () -> csrService.getHistorial(7L, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> csrService.getHistorial(7L, 0, 101, null));
        assertThrows(IllegalArgumentException.class, () -> csrService.getHistorial(7L, 0, 20, "a".repeat(65)));
    }

    @Test
    void getCsrDetails_propietario_devuelveDetalles() {
        when(csrHistorialRepository.findByIdAndUsuarioId(5L, 7L))
                .thenReturn(Optional.of(historialRecord(5L, "detail.com")));

        CsrHistorialResponse dto = csrService.getCsrDetails(5L, 7L);

        assertEquals(5L, dto.getId());
        assertEquals("detail.com", dto.getCn());
    }

    @Test
    void getCsrDetails_noPropietario_lanzaEntityNotFound() {
        when(csrHistorialRepository.findByIdAndUsuarioId(5L, 99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> csrService.getCsrDetails(5L, 99L));
    }
}