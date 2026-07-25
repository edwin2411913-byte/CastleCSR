package com.castlecsr.service;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.dto.CsrHistorialResponse;
import com.castlecsr.exception.CryptographyException;
import com.castlecsr.exception.CsrGenerationException;
import com.castlecsr.model.CsrHistorial;
import com.castlecsr.model.Usuario;
import com.castlecsr.repository.CsrHistorialRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.GeneralName;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CsrService {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    private static final Pattern DNS_PATTERN = Pattern.compile(
            "^(\\*\\.)?[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$");

    private final CryptographyService cryptographyService;
    private final CsrHistorialRepository csrHistorialRepository;

    public CsrService(CryptographyService cryptographyService,
                      CsrHistorialRepository csrHistorialRepository) {
        this.cryptographyService = cryptographyService;
        this.csrHistorialRepository = csrHistorialRepository;
    }

    /** Genera el CSR completo: valida, genera claves, firma, cifra y persiste (sin clave privada). */
    @Transactional
    public CsrGenerationResponse generateCsr(CsrGenerationRequest request, Usuario usuario) {
        validateRequest(request);

        char[] password = request.getPassword().toCharArray();
        KeyPair keyPair = null;
        try {
            String tamanioOCurva;
            if ("RSA".equals(request.getKeyType())) {
                keyPair = cryptographyService.generateRsaKeyPair(request.getKeySize());
                tamanioOCurva = String.valueOf(request.getKeySize());
            } else {
                keyPair = cryptographyService.generateEcKeyPair(request.getCurve());
                tamanioOCurva = request.getCurve();
            }

            X500Name subject = buildX500Name(request);
            GeneralName[] sans = buildSans(request.getSans());

            byte[] csrBytes = cryptographyService.generateCsr(keyPair, subject, sans);
            String csrPem = cryptographyService.convertToPem(csrBytes, "CERTIFICATE REQUEST");

            byte[] encryptedKeyBytes = cryptographyService.encryptPrivateKey(keyPair.getPrivate(), password);
            String encryptedKeyPem = cryptographyService.convertToPem(encryptedKeyBytes, "ENCRYPTED PRIVATE KEY");

            // Persistir en csr_historial: solo metadatos + CSR público, nunca la clave privada
            CsrHistorial record = new CsrHistorial(
                    usuario,
                    request.getCn(),
                    request.getO(),
                    request.getC(),
                    request.getSt(),
                    request.getL(),
                    request.getKeyType(),
                    tamanioOCurva,
                    csrPem);
            record.setUnidadOrganizativa(request.getOu());
            record.setSan(request.getSans() == null || request.getSans().isEmpty()
                    ? null : String.join(",", request.getSans()));
            record = csrHistorialRepository.save(record);

            return new CsrGenerationResponse(String.valueOf(record.getId()), csrPem, encryptedKeyPem);
        } catch (CsrGenerationException | CryptographyException e) {
            throw e;
        } catch (Exception e) {
            throw new CsrGenerationException("Error generando CSR", e);
        } finally {
            // La contraseña se sobrescribe; la clave privada solo vive en la respuesta cifrada
            Arrays.fill(password, '\0');
        }
    }

    /** Historial paginado del usuario, opcionalmente filtrado por CN (case-insensitive). */
    @Transactional(readOnly = true)
    public Page<CsrHistorialResponse> getHistorial(Long usuarioId, int page, int size, String search) {
        if (page < 0) {
            throw new IllegalArgumentException("page debe ser mayor o igual a 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size debe estar entre 1 y 100");
        }
        if (search != null && search.length() > 64) {
            throw new IllegalArgumentException("search no puede exceder 64 caracteres");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<CsrHistorial> historial;
        if (search == null || search.isBlank()) {
            historial = csrHistorialRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId, pageable);
        } else {
            historial = csrHistorialRepository
                    .findByUsuarioIdAndCommonNameContainingIgnoreCaseOrderByCreadoEnDesc(
                            usuarioId, search.trim(), pageable);
        }
        return historial.map(this::convertToCsrHistorialResponse);
    }

    /** Detalle de un CSR: 404 (EntityNotFoundException) si no existe o no pertenece al usuario. */
    @Transactional(readOnly = true)
    public CsrHistorialResponse getCsrDetails(Long csrId, Long usuarioId) {
        CsrHistorial csr = csrHistorialRepository.findByIdAndUsuarioId(csrId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("CSR no encontrado"));
        return convertToCsrHistorialResponse(csr);
    }

    private CsrHistorialResponse convertToCsrHistorialResponse(CsrHistorial csr) {
        return new CsrHistorialResponse(
                csr.getId(),
                csr.getCommonName(),
                csr.getOrganizacion(),
                csr.getAlgoritmo() + "-" + csr.getTamanioOCurva(),
                csr.getCreadoEn());
    }

    private void validateRequest(CsrGenerationRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CsrGenerationException("Las contraseñas no coinciden");
        }

        String country = request.getC().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(country)) {
            throw new CsrGenerationException("Country code no válido (debe ser ISO 3166-1 alpha-2)");
        }

        for (String san : (request.getSans() != null ? request.getSans() : List.<String>of())) {
            if (!isValidSan(san)) {
                throw new CsrGenerationException(
                        "SAN no válido: " + san + " (debe ser example.com, DNS:example.com o IP:192.168.1.1)");
            }
        }

        if ("RSA".equals(request.getKeyType())) {
            if (request.getKeySize() == null) {
                throw new CsrGenerationException("keySize es obligatorio para RSA");
            }
            if (request.getKeySize() != 2048 && request.getKeySize() != 4096) {
                throw new CsrGenerationException("keySize debe ser 2048 o 4096");
            }
        } else if ("EC".equals(request.getKeyType())) {
            if (request.getCurve() == null) {
                throw new CsrGenerationException("curve es obligatorio para EC");
            }
            if (!"secp256r1".equals(request.getCurve()) && !"secp384r1".equals(request.getCurve())) {
                throw new CsrGenerationException("curve debe ser secp256r1 o secp384r1");
            }
        }
    }

    private X500Name buildX500Name(CsrGenerationRequest request) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(BCStyle.C, request.getC().toUpperCase(Locale.ROOT));
        builder.addRDN(BCStyle.ST, request.getSt());
        builder.addRDN(BCStyle.L, request.getL());
        builder.addRDN(BCStyle.O, request.getO());
        if (request.getOu() != null && !request.getOu().isBlank()) {
            builder.addRDN(BCStyle.OU, request.getOu());
        }
        builder.addRDN(BCStyle.CN, request.getCn());

        return builder.build();
    }

    private GeneralName[] buildSans(List<String> sanList) {
        List<GeneralName> sans = new ArrayList<>();
        if (sanList == null) {
            return new GeneralName[0];
        }

        for (String san : sanList) {
            if (san.startsWith("DNS:")) {
                sans.add(new GeneralName(GeneralName.dNSName, san.substring(4)));
            } else if (san.startsWith("IP:")) {
                sans.add(new GeneralName(GeneralName.iPAddress, san.substring(3)));
            } else if (isIpAddress(san)) {
                // Sin prefijo: si es una IP válida se interpreta como IP
                sans.add(new GeneralName(GeneralName.iPAddress, san));
            } else {
                // Sin prefijo y no es IP: se interpreta como DNS
                sans.add(new GeneralName(GeneralName.dNSName, san));
            }
        }

        return sans.toArray(new GeneralName[0]);
    }

    private boolean isValidSan(String san) {
        if (san == null) return false;

        if (san.startsWith("IP:")) {
            return isIpAddress(san.substring(3));
        }

        // Sin prefijo: puede ser una IP o un nombre de dominio
        if (!san.startsWith("DNS:") && isIpAddress(san)) {
            return true;
        }

        String domain = san.startsWith("DNS:") ? san.substring(4) : san;
        return !domain.isEmpty() && domain.length() <= 253 && DNS_PATTERN.matcher(domain).matches();
    }

    private boolean isIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        try {
            // GeneralName parsea IPv4/IPv6 y lanza IllegalArgumentException si es inválida
            new GeneralName(GeneralName.iPAddress, ip);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}