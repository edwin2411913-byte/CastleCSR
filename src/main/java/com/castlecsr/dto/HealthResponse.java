package com.castlecsr.dto;

import java.time.LocalDateTime;

public class HealthResponse {
    private String status;
    private LocalDateTime timestamp;
    private String version;

    public HealthResponse(String status) {
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.version = "1.0.0-SNAPSHOT";
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}