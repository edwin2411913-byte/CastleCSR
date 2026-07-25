package com.castlecsr.dto;

import java.time.LocalDateTime;

public class CsrGenerationResponse {

    private final String csrId;        // id del registro en csr_historial
    private final String csr;          // CSR en formato PEM
    private final String keyEncrypted; // Clave privada cifrada (PKCS#8) en formato PEM
    private final LocalDateTime timestamp;

    public CsrGenerationResponse(String csrId, String csr, String keyEncrypted) {
        this.csrId = csrId;
        this.csr = csr;
        this.keyEncrypted = keyEncrypted;
        this.timestamp = LocalDateTime.now();
    }

    public String getCsrId() { return csrId; }
    public String getCsr() { return csr; }
    public String getKeyEncrypted() { return keyEncrypted; }
    public LocalDateTime getTimestamp() { return timestamp; }
}