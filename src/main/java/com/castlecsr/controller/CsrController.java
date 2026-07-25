package com.castlecsr.controller;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.model.Usuario;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.service.CsrService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build();
        }

        Usuario usuario = userDetailsService.loadUsuarioEntity(auth.getName());
        CsrGenerationResponse response = csrService.generateCsr(request, usuario);

        return ResponseEntity.ok(response);
    }
}