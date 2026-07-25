package com.castlecsr.controller;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.dto.CsrHistorialResponse;
import com.castlecsr.model.Usuario;
import org.springframework.data.domain.Page;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.service.CsrService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csr")
public class CsrController {

    private final CsrService csrService;
    private final CustomUserDetailsService userDetailsService;

    public CsrController(CsrService csrService, CustomUserDetailsService userDetailsService) {
        this.csrService = csrService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/generar")
    public ResponseEntity<CsrGenerationResponse> generarCsr(@Valid @RequestBody CsrGenerationRequest request) {
        Usuario usuario = usuarioAutenticado();
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        CsrGenerationResponse response = csrService.generateCsr(request, usuario);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial")
    public ResponseEntity<Page<CsrHistorialResponse>> historial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Usuario usuario = usuarioAutenticado();
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(csrService.getHistorial(usuario.getId(), page, size, search));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<CsrHistorialResponse> detalles(@PathVariable Long id) {
        Usuario usuario = usuarioAutenticado();
        if (usuario == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(csrService.getCsrDetails(id, usuario.getId()));
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userDetailsService.loadUsuarioEntity(auth.getName());
    }
}