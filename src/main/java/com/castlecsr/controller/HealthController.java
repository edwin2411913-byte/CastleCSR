package com.castlecsr.controller;

import com.castlecsr.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse("OK");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<HealthResponse> info() {
        HealthResponse response = new HealthResponse("CastleCSR Backend is running");
        return ResponseEntity.ok(response);
    }
}