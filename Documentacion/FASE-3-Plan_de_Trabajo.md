# 🔐 Plan de Trabajo - Fase 3: Generación de CSR (Certificate Signing Request)

> **Status:** ✅ COMPLETADA (2026-07-24). Ver `FASE3-COMPLETADA.md` en la raíz del proyecto.
>
> **Cambios post-implementación (2026-07-24):**
> - Los **SANs son opcionales** (se eliminó `@NotEmpty`); sin SANs el CSR se genera sin la extensión SAN y la columna `san` del historial queda en `NULL`.
> - Los **prefijos `DNS:` / `IP:` son opcionales**: `ejemplo.com` y `10.0.0.1` son válidos directamente (auto-detección: IP válida → tipo IP, en otro caso → DNS).
> - Fix frontend: `app.js` ahora envía `passwordConfirm` en el payload de `/api/csr/generar`.

## 🎯 Objetivos

1. Implementar generación de claves criptográficas (RSA y EC) usando BouncyCastle
2. Crear CSR (Certificate Signing Request) en formato PKCS#10
3. Cifrar clave privada con AES y contraseña del usuario
4. Convertir a formato PEM para descarga
5. Validar datos de entrada (CN, O, C, SANs, contraseña)
6. Persistir registro en `csr_historial` (sin clave privada)
7. Alcanzar cobertura de tests unitarios ≥80%

## 📚 Dependencias a agregar

### BouncyCastle

```xml
<!-- Crypto provider (RSA, EC, CSR, AES) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.85</version>
</dependency>

<!-- PKIX (certificados, CSR) -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.85</version>
</dependency>
```

## 🗒️ Tareas Desglosadas

### 🔑 Generación de claves (RSA + EC)

1. Crear método `generateRsaKeyPair(int keySize)` para RSA (2048, 4096 bits)
2. Crear método `generateEcKeyPair(String curve)` para EC (secp256r1, secp384r1)
3. Validar que las claves generadas cumplen estándares criptográficos
4. Limpiar claves de memoria después de enviar respuesta al usuario (sensitive data)

### 📋 Generación de CSR

1. Crear método `generateCSR(KeyPair keyPair, X500Name subject, List<GeneralName> sans)`
2. Implementar PKCS#10 (certificado de solicitud firmado)
3. Incluir extensión SAN (Subject Alternative Name) para dominios/IPs
4. Validar formato de SANs: `example.com`, `192.168.1.1`, `DNS:example.com`, `IP:192.168.1.1` (prefijos opcionales)
5. Firmar CSR con clave privada

### 🔒 Cifrado de clave privada

1. Crear método `encryptPrivateKey(PrivateKey key, String password)` con AES-256
2. Usar PKCS#8 (formato estándar de clave privada cifrada)
3. Derivar clave de cifrado a partir de la contraseña (PBKDF2)
4. Incluir salt aleatorio (256 bits) para seguridad
5. Verificar que solo se puede descifrar con la contraseña correcta

### 🔒 Cifrado de clave privada

1. Crear método `encryptPrivateKey(PrivateKey key, String password)` con AES-256
2. Usar PKCS#8 (formato estándar de clave privada cifrada)
3. Derivar clave de cifrado a partir de la contraseña (PBKDF2)
4. Incluir salt aleatorio (256 bits) para seguridad
5. Verificar que solo se puede descifrar con la contraseña correcta

### 🎨 Conversión a PEM

1. Crear método `convertToPem(byte[] derData, String type)` 
2. Soportar tipos: `CERTIFICATE REQUEST`, `ENCRYPTED PRIVATE KEY`
3. Base64 + líneas de 64 caracteres (estándar PEM)
4. Generar headers y footers correctos

### 📊 DTOs (Data Transfer Objects)

1. Crear `CsrGenerationRequest.java`:
   - `cn` (Common Name): ej. "example.com"
   - `o` (Organization): ej. "ACME Corp"
   - `ou` (Organization Unit): ej. "IT Department"
   - `c` (Country): ej. "MX" (ISO 3166-1 alpha-2)
   - `st` (State): ej. "Mexico City"
   - `l` (Locality): ej. "Mexico City"
   - `sans` (Subject Alternative Names): array de SANs
   - `keyType` (RSA|EC)
   - `keySize` (2048|4096 para RSA)
   - `curve` (secp256r1|secp384r1 para EC)
   - `password` (contraseña para cifrar clave privada)
   - `passwordConfirm` (confirmación de contraseña)

2. Crear `CsrGenerationResponse.java`:
   - `csrId` (UUID del CSR generado)
   - `csr` (CSR en formato PEM, base64)
   - `keyEncrypted` (clave privada cifrada en formato PEM, base64)
   - `timestamp` (fecha de generación)

### ✅ Validaciones

1. **Contraseña:**
   - Mínimo 8 caracteres
   - Contiene mayúsculas, minúsculas, números (opcional pero recomendado)
   - Confirmación coincide

2. **CN (Common Name):**
   - No vacío, máximo 64 caracteres
   - Puede ser dominio o email

3. **País (C):**
   - ISO 3166-1 alpha-2 válido (ej. MX, US, ES)

4. **SANs:**
   - Opcionales; formato válido: `example.com`, `192.168.1.1`, `DNS:example.com` o `IP:192.168.1.1` (prefijos opcionales)
   - IPs deben ser válidas (IPv4 o IPv6)
   - Dominios válidos (sin caracteres especiales inválidos)

5. **keySize / curve:**
   - keySize solo para RSA: 2048 o 4096
   - curve solo para EC: secp256r1 o secp384r1
   - Ambos no pueden ser null al mismo tiempo

### 🧩 Clases nuevas

1. **`CryptographyService.java`**
   - `generateRsaKeyPair(int keySize)`
   - `generateEcKeyPair(String curve)`
   - `generateCSR(KeyPair, X500Name, List<GeneralName>)`
   - `encryptPrivateKey(PrivateKey, String password)`
   - `convertToPem(byte[], String type)`
   - Métodos auxiliares para derivación de claves, salt, etc.

2. **`CsrService.java`**
   - `generateCsr(CsrGenerationRequest request, Long userId)` → devuelve `CsrGenerationResponse`
   - `validateRequest(CsrGenerationRequest)`
   - `buildX500Name(CsrGenerationRequest)` → construye Distinguished Name
   - `buildSans(List<String>)` → convierte SANs a objetos BouncyCastle
   - Limpieza de datos sensibles después de enviar respuesta al usuario

3. **`CsrController.java`**
   - `POST /api/csr/generar` → genera CSR (requiere autenticación)
   - Respuesta JSON con CSR y clave cifrada (en base64)
   - Manejo de excepciones criptográficas

## 🏗️ Diseño Técnico

### 📦 Estructura de clases

```
com.castlecsr/
├── config/
│   └── CryptographyConfig.java (inicializa BouncyCastle)
│
├── service/
│   ├── CryptographyService.java (criptografía pura)
│   └── CsrService.java (orquestación)
│
├── controller/
│   └── CsrController.java (endpoint POST /api/csr/generar)
│
├── dto/
│   ├── CsrGenerationRequest.java
│   └── CsrGenerationResponse.java
│
└── exception/
    ├── CryptographyException.java
    └── CsrGenerationException.java
```

### 🌐 Diagrama de Secuencia - Generación de CSR

```mermaid
sequenceDiagram
    FrontEnd->>CsrController: POST /api/csr/generar
    CsrController->>CsrController: Validar autenticación
    CsrController->>CsrService: generateCsr(request, userId)
    CsrService->>CsrService: validateRequest()
    CsrService->>CryptographyService: generateRsaKeyPair(2048)
    CryptographyService-->>CsrService: KeyPair
    CsrService->>CryptographyService: buildX500Name()
    CryptographyService-->>CsrService: X500Name
    CsrService->>CryptographyService: buildSans()
    CryptographyService-->>CsrService: List<GeneralName>
    CsrService->>CryptographyService: generateCSR(keyPair, x500name, sans)
    CryptographyService-->>CsrService: CSR (PKCS#10)
    CsrService->>CryptographyService: encryptPrivateKey(privateKey, password)
    CryptographyService-->>CsrService: Encrypted PrivateKey
    CsrService->>CryptographyService: convertToPem()
    CryptographyService-->>CsrService: PEM (CSR + Encrypted Key)
    CsrService->>Database: Guardar en csr_historial
    Database-->>CsrService: OK
    CsrService-->>CsrController: CsrGenerationResponse
    CsrController-->>FrontEnd: 200 OK JSON (base64 encoded)
```

### 🔐 Flujo de cifrado de clave privada (AES-256)

```
1. Usuario ingresa contraseña: "MiContraseña123"
   ↓
2. Generar salt aleatorio (256 bits / 32 bytes)
   ↓
3. PBKDF2(password, salt, 100000 iteraciones) → clave de cifrado (256 bits)
   ↓
4. Generar IV (Initialization Vector) aleatorio (128 bits / 16 bytes)
   ↓
5. AES-256-CBC(privateKey, clave, IV) → ciphertext
   ↓
6. PKCS#8 encoded: [SEQUENCE [VERSION, [ALGORITHM, salt], IV, ciphertext]]
   ↓
7. Convertir a PEM:
   -----BEGIN ENCRYPTED PRIVATE KEY-----
   (base64 encoded PKCS#8)
   -----END ENCRYPTED PRIVATE KEY-----
```

## ✅ Criterios de Aceptación

1. Endpoint `POST /api/csr/generar` genera CSR válido (verificable con `openssl req -text`)
2. Clave privada está cifrada y no se puede descifrar sin contraseña
3. CSR incluye extensión SAN con todos los dominios/IPs solicitados
4. Todas las validaciones previenen inyecciones y formatos inválidos
5. Datos sensibles (claves, contraseñas) se limpian de memoria después del uso
6. Registro en BD guarda: id, usuario, CN, O, fecha, algoritmo, tamaño/curva
7. Cobertura de tests ≥80%
8. Errors maneja excepciones de BouncyCastle sin exponer detalles internos

## 🧪 Plan de Pruebas

### Pruebas Unitarias

- **`CryptographyServiceTest`**: 
  - RSA: generación de claves 2048/4096
  - EC: generación de claves secp256r1/secp384r1
  - CSR: formato PKCS#10, SAN incluidos
  - Cifrado: AES-256, PBKDF2, salt único
  - Conversión PEM: headers/footers correctos

- **`CsrServiceTest`**:
  - Validaciones de contraseña (longitud, formato, confirmación)
  - Validaciones de CN, Country
  - Validaciones de SANs (DNS, IP)
  - Persistencia en BD
  - Limpieza de datos sensibles

- **`CsrControllerTest`**:
  - POST /api/csr/generar con datos válidos (200 OK)
  - Sin autenticación (401 Unauthorized)
  - Datos inválidos (400 Bad Request)
  - Respuesta JSON bien formada

### Pruebas de Integración

- **`CsrFlowIntegrationTest`**:
  - Login → Generar CSR RSA → Descargar → Verificar con openssl
  - Login → Generar CSR EC → Descargar → Verificar con openssl
  - Login → Generar con SANs → openssl verifica extensión
  - Verificar que clave privada pide contraseña al descifrar

### Pruebas Manuales (con openssl)

```bash
# Después de descargar csr.pem y key.pem

# Verificar CSR
openssl req -text -noout -in csr.pem

# Debe mostrar:
# - Subject: CN=example.com, O=Company, C=MX, ...
# - Public Key Algorithm: rsaEncryption (2048 bit)
# - Requested Extensions: X509v3 Subject Alternative Name

# Verificar clave privada (debe pedir contraseña)
openssl pkey -in key.pem -text -noout

# Verificar que son compatibles
openssl req -in csr.pem -noout -pubkey > pubkey.pem
openssl pkey -in key.pem -pubout > pubkey_from_key.pem
diff pubkey.pem pubkey_from_key.pem  # Deben ser idénticas
```

## ⏰ Tiempos Estimados

- Implementación `CryptographyService`: 8h
- Implementación `CsrService`: 6h
- Implementación `CsrController`: 4h
- DTOs y validaciones: 4h
- Tests unitarios: 10h
- Tests de integración: 4h
- Pruebas manuales y ajustes: 4h

**Tiempo Total Estimado: 40 horas**

## 📋 Checklist Fase 3

- [ ] Dependencias BouncyCastle (bcprov, bcpkix) agregadas a pom.xml
- [ ] `CryptographyConfig.java` inicializa BouncyCastle como provider
- [ ] `CryptographyService.java` completo con generación RSA/EC
- [ ] `CsrService.java` orquesta generación de CSR
- [ ] `CsrController.java` expone endpoint POST /api/csr/generar
- [ ] DTOs `CsrGenerationRequest` y `CsrGenerationResponse` implementados
- [ ] Validaciones de contraseña (8+ caracteres, confirmación)
- [ ] Validaciones de SANs (DNS:/IP: format)
- [ ] Cifrado AES-256 + PBKDF2 funciona correctamente
- [ ] Claves generadas son verificables con openssl
- [ ] Datos sensibles se limpian de memoria después de enviar respuesta
- [ ] Registro en csr_historial (sin clave privada)
- [ ] Tests unitarios cubren ≥80%
- [ ] Tests de integración cubren flujo completo
- [ ] Verificación manual con openssl exitosa
- [ ] Aprobación de equipo de seguridad
- [ ] Code review completado
- [ ] Documentación de API actualizada
- [ ] Tag v1.0.0-phase3 creado en Git
