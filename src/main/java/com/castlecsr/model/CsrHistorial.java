package com.castlecsr.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "csr_historial", indexes = {
        @Index(name = "idx_csr_historial_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_csr_historial_creado_en", columnList = "creado_en DESC")
})
public class CsrHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 255)
    private String commonName;

    @Column(nullable = false, length = 255)
    private String organizacion;

    @Column(length = 255)
    private String unidadOrganizativa;

    @Column(nullable = false, length = 2)
    private String pais;

    @Column(nullable = false, length = 255)
    private String provincia;

    @Column(nullable = false, length = 255)
    private String localidad;

    @Column(columnDefinition = "TEXT")
    private String san;

    @Column(nullable = false, length = 10)
    private String algoritmo;

    @Column(nullable = false, length = 20)
    private String tamanioOCurva;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String csrPem;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public CsrHistorial() {
    }

    public CsrHistorial(Usuario usuario, String commonName, String organizacion,
                        String pais, String provincia, String localidad,
                        String algoritmo, String tamanioOCurva, String csrPem) {
        this.usuario = usuario;
        this.commonName = commonName;
        this.organizacion = organizacion;
        this.pais = pais;
        this.provincia = provincia;
        this.localidad = localidad;
        this.algoritmo = algoritmo;
        this.tamanioOCurva = tamanioOCurva;
        this.csrPem = csrPem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getOrganizacion() {
        return organizacion;
    }

    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

    public String getUnidadOrganizativa() {
        return unidadOrganizativa;
    }

    public void setUnidadOrganizativa(String unidadOrganizativa) {
        this.unidadOrganizativa = unidadOrganizativa;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public void setAlgoritmo(String algoritmo) {
        this.algoritmo = algoritmo;
    }

    public String getTamanioOCurva() {
        return tamanioOCurva;
    }

    public void setTamanioOCurva(String tamanioOCurva) {
        this.tamanioOCurva = tamanioOCurva;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public String toString() {
        return "CsrHistorial{" +
                "id=" + id +
                ", commonName='" + commonName + '\'' +
                ", organizacion='" + organizacion + '\'' +
                ", algoritmo='" + algoritmo + '\'' +
                ", creadoEn=" + creadoEn +
                '}';
    }
}