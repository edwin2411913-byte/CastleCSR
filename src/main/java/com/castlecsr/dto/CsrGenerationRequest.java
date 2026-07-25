package com.castlecsr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CsrGenerationRequest {

    @NotBlank(message = "CN (Common Name) es obligatorio")
    @Size(max = 64, message = "CN no puede exceder 64 caracteres")
    private String cn;

    @NotBlank(message = "O (Organization) es obligatorio")
    @Size(max = 255)
    private String o;

    @Size(max = 255)
    private String ou; // Opcional

    @NotBlank(message = "C (Country) es obligatorio")
    @Size(min = 2, max = 2, message = "C debe ser código ISO 3166-1 alpha-2 (ej. MX)")
    private String c;

    // provincia y localidad son NOT NULL en csr_historial
    @NotBlank(message = "ST (State/Provincia) es obligatorio")
    @Size(max = 255)
    private String st;

    @NotBlank(message = "L (Locality) es obligatorio")
    @Size(max = 255)
    private String l;

    private List<String> sans; // Opcional - ej. ["DNS:example.com", "IP:192.168.1.1"]

    @NotBlank(message = "keyType es obligatorio (RSA|EC)")
    @Pattern(regexp = "RSA|EC", message = "keyType debe ser RSA o EC")
    private String keyType;

    // Para RSA: 2048 o 4096
    private Integer keySize;

    // Para EC: secp256r1 o secp384r1
    private String curve;

    @NotBlank(message = "Password es obligatorio")
    @Size(min = 8, message = "Password debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "passwordConfirm es obligatorio")
    private String passwordConfirm;

    public String getCn() { return cn; }
    public void setCn(String cn) { this.cn = cn; }

    public String getO() { return o; }
    public void setO(String o) { this.o = o; }

    public String getOu() { return ou; }
    public void setOu(String ou) { this.ou = ou; }

    public String getC() { return c; }
    public void setC(String c) { this.c = c; }

    public String getSt() { return st; }
    public void setSt(String st) { this.st = st; }

    public String getL() { return l; }
    public void setL(String l) { this.l = l; }

    public List<String> getSans() { return sans; }
    public void setSans(List<String> sans) { this.sans = sans; }

    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }

    public Integer getKeySize() { return keySize; }
    public void setKeySize(Integer keySize) { this.keySize = keySize; }

    public String getCurve() { return curve; }
    public void setCurve(String curve) { this.curve = curve; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
}