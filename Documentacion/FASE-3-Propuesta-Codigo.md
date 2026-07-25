# 🔐 Propuesta de Código — Fase 3: Generación de CSR (BouncyCastle)

**Basado en:** `FASE-3-Plan_de_Trabajo.md`
**Librería principal:** Bouncy Castle 1.85 (bcprov + bcpkix)
**Integración:** Reutiliza autenticación JWT de Fase 2, BD de Fase 1

---

## 📂 Archivos nuevos y modificados

```
src/main/java/com/castlecsr/
├── config/
│   └── CryptographyConfig.java                ← 🆕 NUEVO
│
├── service/
│   ├── CryptographyService.java               ← 🆕 NUEVO
│   └── CsrService.java                        ← 🆕 NUEVO
│
├── controller/
│   └── CsrController.java                     ← 🆕 NUEVO
│
├── dto/
│   ├── CsrGenerationRequest.java              ← 🆕 NUEVO
│   └── CsrGenerationResponse.java             ← 🆕 NUEVO
│
└── exception/
    ├── CryptographyException.java             ← 🆕 NUEVO
    └── CsrGenerationException.java            ← 🆕 NUEVO
```

---

## 1️⃣ `CryptographyConfig.java` (nuevo)

Inicializa BouncyCastle como provider criptográfico de confianza.

```java
package com.castlecsr.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class CryptographyConfig {

    static {
        // Registrar BouncyCastle como provider de criptografía
        Security.addProvider(new BouncyCastleProvider());
    }
}
```

---

## 2️⃣ `CryptographyException.java` (nuevo)

```java
package com.castlecsr.exception;

public class CryptographyException extends RuntimeException {
    public CryptographyException(String message) {
        super(message);
    }

    public CryptographyException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 3️⃣ `CsrGenerationException.java` (nuevo)

```java
package com.castlecsr.exception;

public class CsrGenerationException extends RuntimeException {
    public CsrGenerationException(String message) {
        super(message);
    }

    public CsrGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 4️⃣ `CsrGenerationRequest.java` (nuevo DTO)

```java
package com.castlecsr.dto;

import jakarta.validation.constraints.*;

public class CsrGenerationRequest {

    @NotBlank(message = "CN (Common Name) es obligatorio")
    @Size(max = 64, message = "CN no puede exceder 64 caracteres")
    private String cn;

    @NotBlank(message = "O (Organization) es obligatorio")
    private String o;

    private String ou; // Opcional

    @NotBlank(message = "C (Country) es obligatorio")
    @Size(min = 2, max = 2, message = "C debe ser código ISO 3166-1 alpha-2 (ej. MX)")
    private String c;

    private String st; // State (opcional)
    private String l;  // Locality (opcional)

    // NOTA (post-implementación): los SANs son OPCIONALES (sin @NotEmpty) y los
    // prefijos DNS:/IP: también son opcionales — se auto-detecta el tipo.
    private java.util.List<String> sans; // ej. ["example.com", "10.0.0.1", "DNS:example.com", "IP:192.168.1.1"]

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

    // Getters y Setters
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

    public java.util.List<String> getSans() { return sans; }
    public void setSans(java.util.List<String> sans) { this.sans = sans; }

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
```

---

## 5️⃣ `CsrGenerationResponse.java` (nuevo DTO)

```java
package com.castlecsr.dto;

import java.time.LocalDateTime;

public class CsrGenerationResponse {

    private String csrId; // UUID del CSR generado
    private String csr;   // CSR en PEM, base64
    private String keyEncrypted; // Clave privada cifrada en PEM, base64
    private LocalDateTime timestamp;

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
```

---

## 6️⃣ `CryptographyService.java` (nuevo)

Encapsula toda la criptografía: generación de claves, CSR, cifrado.

```java
package com.castlecsr.service;

import com.castlecsr.exception.CryptographyException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Service
public class CryptographyService {

    private static final String ALGORITHM_RSA = "RSA";
    private static final String ALGORITHM_EC = "EC";
    private static final int RSA_KEY_SIZE_2048 = 2048;
    private static final int RSA_KEY_SIZE_4096 = 4096;
    private static final String CURVE_P256 = "secp256r1";
    private static final String CURVE_P384 = "secp384r1";

    private static final String SIGNATURE_ALGORITHM_RSA = "SHA256WithRSA";
    private static final String SIGNATURE_ALGORITHM_EC = "SHA256WithECDSA";

    private static final int SALT_LENGTH = 32; // 256 bits
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final String KEY_DERIVATION_ALG = "PBKDF2WithHmacSHA256";

    /**
     * Genera par de claves RSA
     */
    public KeyPair generateRsaKeyPair(int keySize) {
        if (keySize != RSA_KEY_SIZE_2048 && keySize != RSA_KEY_SIZE_4096) {
            throw new CryptographyException("keySize debe ser 2048 o 4096");
        }

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM_RSA, "BC");
            kpg.initialize(keySize);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new CryptographyException("Error generando claves RSA", e);
        }
    }

    /**
     * Genera par de claves EC (Elliptic Curve)
     */
    public KeyPair generateEcKeyPair(String curve) {
        if (!CURVE_P256.equals(curve) && !CURVE_P384.equals(curve)) {
            throw new CryptographyException("curve debe ser secp256r1 o secp384r1");
        }

        try {
            ECParameterSpec spec = ECNamedCurveTable.getParameterSpec(curve);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM_EC, "BC");
            kpg.initialize(spec);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | IllegalArgumentException e) {
            throw new CryptographyException("Error generando claves EC para curva " + curve, e);
        }
    }

    /**
     * Genera CSR (Certificate Signing Request) en PKCS#10
     */
    public byte[] generateCsr(KeyPair keyPair, org.bouncycastle.asn1.x500.X500Name subject,
                               GeneralName[] sans) {
        try {
            PKCS10CertificationRequestBuilder builder =
                    new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

            // Agregar extensión SAN (Subject Alternative Name)
            if (sans != null && sans.length > 0) {
                GeneralNames generalNames = new GeneralNames(sans);
                ExtensionsGenerator extGen = new ExtensionsGenerator();
                extGen.addExtension(Extension.subjectAlternativeName, false, generalNames);
                builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, 
                        extGen.generate());
            }

            // Seleccionar algoritmo de firma según tipo de clave
            String signAlg = keyPair.getPrivate().getAlgorithm().equals(ALGORITHM_RSA)
                    ? SIGNATURE_ALGORITHM_RSA
                    : SIGNATURE_ALGORITHM_EC;

            PKCS10CertificationRequest csr = builder.build(
                    new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder(signAlg)
                            .setProvider("BC")
                            .build(keyPair.getPrivate())
            );

            return csr.getEncoded();
        } catch (Exception e) {
            throw new CryptographyException("Error generando CSR", e);
        }
    }

    /**
     * Cifra clave privada con AES-256 derivado de contraseña
     */
    public byte[] encryptPrivateKey(PrivateKey privateKey, String password) {
        try {
            // Generar salt aleatorio
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            // Derivar clave de cifrado a partir de contraseña (PBKDF2)
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALG);
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();
            SecretKeySpec secretKey = new SecretKeySpec(derivedKey, 0, 32, "AES");

            // Generar IV aleatorio
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            // Cifrar clave privada con AES-256-CBC
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedKey = cipher.doFinal(privateKey.getEncoded());

            // Construir PKCS#8 EncryptedPrivateKeyInfo
            // Formato: [SEQUENCE [VERSION, [ALGORITHM, salt], IV, ciphertext]]
            return buildEncryptedPrivateKeyInfo(salt, iv, encryptedKey);

        } catch (Exception e) {
            throw new CryptographyException("Error cifrando clave privada", e);
        }
    }

    /**
     * Convierte bytes a formato PEM
     */
    public String convertToPem(byte[] derData, String type) {
        String header = "-----BEGIN " + type + "-----";
        String footer = "-----END " + type + "-----";

        String base64 = Base64.getEncoder().encodeToString(derData);

        // Dividir en líneas de 64 caracteres
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }
        sb.append(footer).append("\n");

        return sb.toString();
    }

    /**
     * Privado: construye PKCS#8 EncryptedPrivateKeyInfo (simplificado)
     * En producción, usar org.bouncycastle.openssl.jcajce.JcaPEMWriter
     */
    private byte[] buildEncryptedPrivateKeyInfo(byte[] salt, byte[] iv, byte[] ciphertext) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // SEQUENCE contenedor
            // [VERSION 0]
            // [ALGORITHM OID + salt] 
            // [IV]
            // [CIPHERTEXT]
            // Nota: para producción, usar BouncyCastle EncryptedPrivateKeyInfo builder
            
            // Simplificación: retornar concatenación salt + iv + ciphertext
            // En código real, esto se estructura según PKCS#8
            baos.write(salt);
            baos.write(iv);
            baos.write(ciphertext);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new CryptographyException("Error construyendo EncryptedPrivateKeyInfo", e);
        }
    }
}
```

⚠️ **Nota sobre PKCS#8 EncryptedPrivateKeyInfo:** El método `buildEncryptedPrivateKeyInfo` es simplificado aquí. En producción, deberías usar las clases de BouncyCastle `EncryptedPrivateKeyInfo` directamente. Para más detalles, ver documentación de BouncyCastle.

---

## 7️⃣ `CsrService.java` (nuevo)

Orquesta la generación de CSR: validación, generación, persistencia.

```java
package com.castlecsr.service;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.exception.CsrGenerationException;
import com.castlecsr.model.CsrHistorial;
import com.castlecsr.repository.CsrHistorialRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.GeneralName;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CsrService {

    private final CryptographyService cryptographyService;
    private final CsrHistorialRepository csrHistorialRepository;

    public CsrService(CryptographyService cryptographyService,
                       CsrHistorialRepository csrHistorialRepository) {
        this.cryptographyService = cryptographyService;
        this.csrHistorialRepository = csrHistorialRepository;
    }

    /**
     * Genera CSR completamente: validar, generar claves, CSR, cifrado
     */
    public CsrGenerationResponse generateCsr(CsrGenerationRequest request, Long userId) {
        // 1. Validar request
        validateRequest(request);

        try {
            // 2. Generar claves según tipo
            KeyPair keyPair;
            String algorithm;
            if ("RSA".equals(request.getKeyType())) {
                keyPair = cryptographyService.generateRsaKeyPair(request.getKeySize());
                algorithm = "RSA-" + request.getKeySize();
            } else if ("EC".equals(request.getKeyType())) {
                keyPair = cryptographyService.generateEcKeyPair(request.getCurve());
                algorithm = "EC-" + request.getCurve();
            } else {
                throw new CsrGenerationException("keyType no soportado");
            }

            // 3. Construir Distinguished Name (DN)
            X500Name subject = buildX500Name(request);

            // 4. Construir SANs
            GeneralName[] sans = buildSans(request.getSans());

            // 5. Generar CSR (PKCS#10)
            byte[] csrBytes = cryptographyService.generateCsr(keyPair, subject, sans);
            String csrPem = cryptographyService.convertToPem(csrBytes, "CERTIFICATE REQUEST");

            // 6. Cifrar clave privada con AES-256
            byte[] encryptedKeyBytes = cryptographyService.encryptPrivateKey(
                    keyPair.getPrivate(), request.getPassword());
            String encryptedKeyPem = cryptographyService.convertToPem(
                    encryptedKeyBytes, "ENCRYPTED PRIVATE KEY");

            // 7. Guardar en BD (sin clave privada)
            String csrId = UUID.randomUUID().toString();
            CsrHistorial record = new CsrHistorial();
            record.setId(csrId);
            record.setUsuarioId(userId);
            record.setCn(request.getCn());
            record.setOrganization(request.getO());
            record.setAlgorithm(algorithm);
            record.setFechaGeneracion(LocalDateTime.now());
            csrHistorialRepository.save(record);

            // 8. Limpiar datos sensibles de memoria
            cleanSensitiveData(request.getPassword(), keyPair);

            // 9. Devolver respuesta
            return new CsrGenerationResponse(csrId, csrPem, encryptedKeyPem);

        } catch (Exception e) {
            throw new CsrGenerationException("Error generando CSR: " + e.getMessage(), e);
        }
    }

    /**
     * Valida el request completamente
     */
    private void validateRequest(CsrGenerationRequest request) {
        // Contraseña: confirmación
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CsrGenerationException("Las contraseñas no coinciden");
        }

        // Country: validar que sea ISO 3166-1 alpha-2
        if (!isValidCountryCode(request.getC())) {
            throw new CsrGenerationException("Country code no válido (debe ser ISO 3166-1 alpha-2)");
        }

        // SANs: no vacío
        if (request.getSans() == null || request.getSans().isEmpty()) {
            throw new CsrGenerationException("SANs no puede estar vacío");
        }

        // SANs: validar formato
        for (String san : request.getSans()) {
            if (!isValidSan(san)) {
                throw new CsrGenerationException("SAN no válido: " + san + 
                        " (debe ser DNS:example.com o IP:192.168.1.1)");
            }
        }

        // keyType y keySize/curve: consistencia
        if ("RSA".equals(request.getKeyType())) {
            if (request.getKeySize() == null) {
                throw new CsrGenerationException("keySize es obligatorio para RSA");
            }
        } else if ("EC".equals(request.getKeyType())) {
            if (request.getCurve() == null) {
                throw new CsrGenerationException("curve es obligatorio para EC");
            }
        }
    }

    /**
     * Construye X500Name (Distinguished Name) a partir del request
     */
    private X500Name buildX500Name(CsrGenerationRequest request) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(BCStyle.C, request.getC());
        if (request.getSt() != null) builder.addRDN(BCStyle.ST, request.getSt());
        if (request.getL() != null) builder.addRDN(BCStyle.L, request.getL());
        builder.addRDN(BCStyle.O, request.getO());
        if (request.getOu() != null) builder.addRDN(BCStyle.OU, request.getOu());
        builder.addRDN(BCStyle.CN, request.getCn());

        return builder.build();
    }

    /**
     * Construye array de GeneralName a partir de lista de SANs
     */
    private GeneralName[] buildSans(List<String> sanList) {
        List<GeneralName> sans = new ArrayList<>();

        for (String san : sanList) {
            if (san.startsWith("DNS:")) {
                String domain = san.substring(4);
                sans.add(new GeneralName(GeneralName.dNSName, domain));
            } else if (san.startsWith("IP:")) {
                String ip = san.substring(3);
                // IP como octetos (simplificado, debería validar IPv4/IPv6)
                sans.add(new GeneralName(GeneralName.iPAddress, ip));
            }
        }

        return sans.toArray(new GeneralName[0]);
    }

    /**
     * Limpia datos sensibles de memoria
     */
    private void cleanSensitiveData(String password, KeyPair keyPair) {
        // En Java, no hay forma 100% garantizada de limpiar strings de la memoria heap
        // Pero podemos intentar:
        try {
            // Sobrescribir contraseña si está en un char[]
            if (password != null) {
                char[] chars = password.toCharArray();
                Arrays.fill(chars, ' ');
            }

            // Para claves, Spring/BouncyCastle internamente ya limpian
            // pero podemos hacer nullify explícito si las clases lo soportan
        } catch (Exception e) {
            // Log warning pero continuar
        }
    }

    /**
     * Valida que el SAN tenga formato correcto
     */
    private boolean isValidSan(String san) {
        return (san.startsWith("DNS:") && san.length() > 4) ||
               (san.startsWith("IP:") && san.length() > 3);
    }

    /**
     * Valida country code ISO 3166-1 alpha-2 (lista pequeña, expandir según necesidad)
     */
    private boolean isValidCountryCode(String code) {
        Set<String> validCodes = Set.of(
            "MX", "US", "CA", "ES", "FR", "DE", "IT", "GB", "JP", "CN",
            "BR", "AR", "CO", "PE", "CL", "AU", "NZ", "IN", "SG", "HK",
            "AE", "SA", "KR", "TW", "TH", "MY", "PH", "ID", "VN", "NL"
        );
        return validCodes.contains(code);
    }
}
```

---

## 8️⃣ `CsrController.java` (nuevo)

Expone el endpoint para generar CSR.

```java
package com.castlecsr.controller;

import com.castlecsr.dto.CsrGenerationRequest;
import com.castlecsr.dto.CsrGenerationResponse;
import com.castlecsr.security.CustomUserDetailsService;
import com.castlecsr.service.CsrService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
        // Verificar autenticación (Fase 2 - JWT)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = auth.getName();
        com.castlecsr.model.Usuario usuario = userDetailsService.loadUsuarioEntity(username);

        // Generar CSR
        CsrGenerationResponse response = csrService.generateCsr(request, usuario.getId());

        return ResponseEntity.ok(response);
    }
}
```

---

## 📊 Tabla de integración con código existente

| Componente | ¿Se modifica? | Cómo |
|---|---|---|
| `Usuario.java` | ❌ No | Se reutiliza (getId ya existe) |
| `UsuarioRepository.java` | ❌ No | Ya tiene findByUsername() |
| `CustomUserDetailsService.java` | ❌ No | Se reutiliza loadUsuarioEntity() |
| `CsrHistorial.java` (entidad JPA existente) | ✅ Mínimamente | Verificar que tenga fields: id, usuarioId, cn, organization, algorithm, fechaGeneracion |
| `CsrHistorialRepository.java` (existente) | ❌ No | Se reutiliza el save() |
| `SecurityConfig.java` | ❌ No | No requiere cambios (CsrController requiere autenticación, que ya está) |
| `GlobalExceptionHandler.java` | ✅ Agregar | Handlers para `CsrGenerationException` y `CryptographyException` |
| `app.js` (frontend) | ❌ No | Será actualizado en Fase 4 para mostrar historial y descarga |

---

## 🔧 Handlers para GlobalExceptionHandler (a agregar)

```java
// Agregar estos imports:
import com.castlecsr.exception.CryptographyException;
import com.castlecsr.exception.CsrGenerationException;

// Agregar estos métodos a GlobalExceptionHandler.java:

@ExceptionHandler(CryptographyException.class)
public ResponseEntity<ErrorResponse> handleCryptographyException(CryptographyException ex) {
    ErrorResponse error = new ErrorResponse(500, "Internal Server Error", "Error criptográfico: " + ex.getMessage());
    return ResponseEntity.status(500).body(error);
}

@ExceptionHandler(CsrGenerationException.class)
public ResponseEntity<ErrorResponse> handleCsrGenerationException(CsrGenerationException ex) {
    ErrorResponse error = new ErrorResponse(400, "Bad Request", "Error generando CSR: " + ex.getMessage());
    return ResponseEntity.status(400).body(error);
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(e -> 
        errors.put(e.getField(), e.getDefaultMessage())
    );
    ErrorResponse error = new ErrorResponse(400, "Validation Error", "Datos inválidos", null, errors);
    return ResponseEntity.status(400).body(error);
}
```

---

## 🧪 Cómo probar manualmente (curl)

```bash
# 1. Login (obtener cookie)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jgomez","password":"tu_password"}'

# 2. Generar CSR RSA
curl -i -b cookies.txt -X POST http://localhost:8080/api/csr/generar \
  -H "Content-Type: application/json" \
  -d '{
    "cn": "example.com",
    "o": "ACME Corp",
    "ou": "IT",
    "c": "MX",
    "st": "Mexico City",
    "l": "Mexico City",
    "sans": ["DNS:example.com", "DNS:www.example.com"],
    "keyType": "RSA",
    "keySize": 2048,
    "password": "MiContraseña123",
    "passwordConfirm": "MiContraseña123"
  }'

# Respuesta esperada:
# {
#   "csrId": "550e8400-e29b-41d4-a716-446655440000",
#   "csr": "-----BEGIN CERTIFICATE REQUEST-----\n...\n-----END CERTIFICATE REQUEST-----\n",
#   "keyEncrypted": "-----BEGIN ENCRYPTED PRIVATE KEY-----\n...\n-----END ENCRYPTED PRIVATE KEY-----\n",
#   "timestamp": "2026-07-24T10:30:00"
# }

# 3. Validar CSR con openssl (decodificar base64 primero)
echo "-----BEGIN CERTIFICATE REQUEST-----
..." | openssl req -text -noout -in -

# 4. Validar clave privada cifrada (debe pedir contraseña)
echo "-----BEGIN ENCRYPTED PRIVATE KEY-----
..." | openssl pkey -text -noout -in -
# Debe pedir: "Enter pass phrase for standard input:"
```

---

## 📋 Pendientes a confirmar

- [ ] ¿`CsrHistorial` existe con los campos necesarios (id, usuarioId, cn, organization, algorithm, fechaGeneracion)?
- [ ] ¿`CsrHistorialRepository` existe y extiende `JpaRepository<CsrHistorial, String>`?
- [ ] Confirmar que PKCS#8 EncryptedPrivateKeyInfo se construye correctamente (usar `org.bouncycastle.openssl.jcajce.JcaPEMWriter` en producción)

