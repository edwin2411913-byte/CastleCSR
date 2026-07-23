package com.castlecsr.dto;

public class SessionResponse {
    private Long id;
    private String username;
    private String rol;

    public SessionResponse(Long id, String username, String rol) {
        this.id = id;
        this.username = username;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRol() {
        return rol;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}