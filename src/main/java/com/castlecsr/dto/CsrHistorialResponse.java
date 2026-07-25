package com.castlecsr.dto;

import java.time.LocalDateTime;

/** Respuesta del historial de CSRs: solo metadatos, nunca claves privadas. */
public class CsrHistorialResponse {

    private Long id;
    private String cn;
    private String organization;
    private String algorithm;
    private LocalDateTime fechaGeneracion;

    public CsrHistorialResponse() {
    }

    public CsrHistorialResponse(Long id, String cn, String organization,
                                String algorithm, LocalDateTime fechaGeneracion) {
        this.id = id;
        this.cn = cn;
        this.organization = organization;
        this.algorithm = algorithm;
        this.fechaGeneracion = fechaGeneracion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    @Override
    public String toString() {
        return "CsrHistorialResponse{" +
                "id=" + id +
                ", cn='" + cn + '\'' +
                ", organization='" + organization + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", fechaGeneracion=" + fechaGeneracion +
                '}';
    }
}