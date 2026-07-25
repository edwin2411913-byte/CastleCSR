# 🎉 FASE 3 COMPLETADA - CastleCSR Backend

**Fecha de Finalización:** 2026-07-24  
**Status:** ✅ COMPLETADO  
**Rama:** `main`

---

## 📋 Resumen de Implementación

La Fase 3 agrega la **generación de CSRs (Certificate Signing Requests)** en formato PKCS#10 usando **BouncyCastle**. El usuario autenticado puede generar pares de claves **RSA (2048/4096)** o **EC (secp256r1/secp384r1)**, obtener el CSR en PEM y descargar su clave privada **cifrada con AES-256 (PKCS#8 + PBKDF2)** protegida por contraseña. Cada generación se registra en `csr_historial` (sin la clave privada).

### ✅ Estructura del Proyecto (nuevos componentes)
```
src/main/java/com/castlecsr/
├── config/
│   └── CryptographyConfig.java            (NUEVO: registra provider BouncyCastle)
├── controller/
│   └── CsrController.java                 (NUEVO: POST /api/csr/generar)
├── dto/
│   ├── CsrGenerationRequest.java          (NUEVO: DN + SANs + keyType + password)
│   └── CsrGenerationResponse.java         (NUEVO: id, csr PEM, key cifrada PEM)
├── exception/
│   ├── CryptographyException.java         (NUEVO)
│   ├── CsrGenerationException.java        (NUEVO)
│   └── GlobalExceptionHandler.java        (ACTUALIZADO: errores de CSR/crypto)
└── service/                               (PAQUETE NUEVO)
    ├── CryptographyService.java           (Claves RSA/EC, CSR PKCS#10, AES-256, PEM)
    └── CsrService.java                    (Validación, orquestación, persistencia)

src/main/resources/static/
├── index.html                             (ACTUALIZADO: formulario de generación CSR)
└── js/app.js                              (ACTUALIZADO: validación + fetch + descargas)
```

### 📦 Dependencias Agregadas
- **bcprov-jdk18on 1.85** (BouncyCastle provider: RSA, EC, AES)
- **bcpkix-jdk18on 1.85** (PKIX: CSR PKCS#10, PEM)

---

## 🔐 Diseño de Generación de CSR

### Flujo
```
1. POST /api/csr/generar  {cn, o, ou?, c, st, l, sans?, keyType, keySize|curve, password, passwordConfirm}
2. CsrService valida entrada (país ISO, SANs, passwords coinciden, keySize/curve)
3. CryptographyService genera el par de claves (RSA o EC)
4. Se construye el CSR PKCS#10 con subject DN y extensión SAN (si hay SANs)
5. La clave privada se cifra con AES-256 (PBKDF2 + salt) usando la contraseña
6. Se persiste el registro en csr_historial (sin clave privada)
7. Respuesta: {id, csr (PEM), keyEncrypted (PEM cifrado)}
```

### Campos del Request
| Campo | Obligatorio | Detalle |
|-------|-------------|---------|
| `cn` | ✅ | Common Name (máx. 64) |
| `o` | ✅ | Organization |
| `ou` | ❌ | Organizational Unit |
| `c` | ✅ | País ISO 3166-1 alpha-2 (ej. `MX`) |
| `st` | ✅ | Estado/Provincia |
| `l` | ✅ | Localidad |
| `sans` | ❌ | **Opcionales.** Lista de SANs (ver formatos abajo) |
| `keyType` | ✅ | `RSA` o `EC` |
| `keySize` | RSA | `2048` o `4096` |
| `curve` | EC | `secp256r1` o `secp384r1` |
| `password` | ✅ | Mín. 8 caracteres (cifra la clave privada) |
| `passwordConfirm` | ✅ | Debe coincidir con `password` |

### Formatos de SAN aceptados
| Entrada | Interpretación |
|---------|----------------|
| `ejemplo.com.mx` | DNS (auto-detectado, **sin prefijo**) |
| `10.0.0.1` | IP (auto-detectada, **sin prefijo**) |
| `DNS:ejemplo.com` | DNS (prefijo explícito) |
| `IP:10.0.0.1` | IP (prefijo explícito) |

> Los prefijos `DNS:` e `IP:` son **opcionales**: sin prefijo, si el valor es una IP válida (IPv4/IPv6) se trata como IP; en caso contrario se valida como nombre de dominio.  
> Si no se envían SANs, el CSR se genera **sin la extensión** Subject Alternative Name.

### Criptografía
| Aspecto | Implementación |
|---------|----------------|
| Claves RSA | 2048 / 4096 bits |
| Claves EC | secp256r1 / secp384r1 |
| CSR | PKCS#10 firmado con la clave privada (SHA256withRSA / SHA256withECDSA) |
| Cifrado de clave | AES-256, PKCS#8, derivación PBKDF2 con salt aleatorio |
| Formato de salida | PEM (`CERTIFICATE REQUEST` y `ENCRYPTED PRIVATE KEY`) |

---

## 📡 Endpoints Implementados

### Protegido (requiere cookie JWT válida)
```bash
POST /api/csr/generar      → Genera CSR + clave privada cifrada
```

### Ejemplo con curl
```bash
# Login previo (Fase 2)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu_password"}'

# Generar CSR (SANs opcionales y sin prefijo)
curl -b cookies.txt -X POST http://localhost:8080/api/csr/generar \
  -H "Content-Type: application/json" \
  -d '{
    "cn": "ejemplo.com.mx",
    "o": "Mi Empresa",
    "c": "MX",
    "st": "CDMX",
    "l": "Ciudad de Mexico",
    "sans": ["ejemplo.com.mx", "www.ejemplo.com.mx", "10.0.0.1"],
    "keyType": "RSA",
    "keySize": 2048,
    "password": "MiPassword123",
    "passwordConfirm": "MiPassword123"
  }'
# → {"id":"1","csr":"-----BEGIN CERTIFICATE REQUEST-----...","keyEncrypted":"-----BEGIN ENCRYPTED PRIVATE KEY-----..."}
```

---

## 🧪 Tests

**12 clases de test, 56 tests en total** (23 nuevos en Fase 3):

| Clase | Tests | Qué verifica |
|-------|-------|--------------|
| `CryptographyServiceTest` | 8 | Claves RSA/EC, CSR PKCS#10, cifrado AES, conversión PEM |
| `CsrServiceTest` | 7 | Validaciones (país, SANs, passwords, keySize/curve), persistencia |
| `CsrControllerTest` | 4 | Endpoint, validación de request, respuestas de error |
| `CsrFlowIntegrationTest` | 4 | Flujo completo: login → generar CSR → registro en historial |
| *(Fases 1-2)* | 33 | Autenticación JWT, cookies, health, contexto |

```bash
mvn test
```

---

## 🔧 Cambios Post-Implementación (2026-07-24)

Ajustes de usabilidad aplicados después de la implementación inicial:

1. **SANs opcionales:** se eliminó `@NotEmpty` en `CsrGenerationRequest.sans`. Se puede generar un CSR sin SANs; en ese caso no se incluye la extensión SAN y la columna `san` del historial queda en `NULL`.
2. **SANs sin prefijo:** `ejemplo.com.mx` y `10.0.0.1` son válidos directamente. La detección es automática (IP válida → tipo IP; en otro caso → DNS). Los prefijos `DNS:` / `IP:` siguen soportados.
3. **Fix frontend:** `app.js` no enviaba `passwordConfirm` en el payload, provocando 400 del backend. Corregido.
4. **Textos de UI:** placeholder y ayuda del campo SAN actualizados (`ejemplo.com o 10.0.0.1`, prefijos opcionales).

---

## 🔐 Seguridad

### Implementado en Fase 3
- ✅ Endpoint protegido por JWT (solo usuarios autenticados)
- ✅ Clave privada **nunca se persiste** (solo viaja cifrada en la respuesta)
- ✅ Cifrado AES-256 con PBKDF2 + salt aleatorio
- ✅ Validación estricta de entrada (país ISO, SANs, tamaños de clave, curvas)
- ✅ Contraseña mínima de 8 caracteres, confirmación obligatoria
- ✅ Mensajes de error controlados vía `GlobalExceptionHandler`

### No Implementado (fases futuras)
- ❌ Historial consultable con paginación (Fase 4)
- ❌ Rate limiting por usuario
- ❌ Políticas de complejidad de contraseña avanzadas

---

## 📊 Estado del Proyecto

### Checklist de Fase 3
- ✅ BouncyCastle 1.85 integrado (bcprov + bcpkix)
- ✅ Generación de claves RSA (2048/4096) y EC (secp256r1/secp384r1)
- ✅ CSR PKCS#10 en PEM con extensión SAN opcional
- ✅ Cifrado de clave privada AES-256 (PKCS#8 + PBKDF2)
- ✅ `POST /api/csr/generar` protegido con JWT
- ✅ Persistencia en `csr_historial` asociada al usuario
- ✅ SANs opcionales y con prefijos DNS:/IP: opcionales
- ✅ Frontend: formulario de generación + descarga de .csr y .key
- ✅ 23 tests nuevos (56 en total)
- ✅ Documentación actualizada

---

## 💡 Troubleshooting

### "SAN no válido: ..."
- Formatos aceptados: `ejemplo.com`, `10.0.0.1`, `DNS:ejemplo.com`, `IP:10.0.0.1`
- Los prefijos son opcionales; una IP inválida sin prefijo se valida como dominio

### "passwordConfirm es obligatorio" (400)
- El payload debe incluir `password` **y** `passwordConfirm` (y deben coincidir)
- Si usas el frontend, recarga con Ctrl+F5 para tomar el `app.js` actualizado

### La app sigue pidiendo el formato con prefijos
- La JVM en ejecución tiene código antiguo: **reinicia** la aplicación (`mvn spring-boot:run`)
- Limpia caché del navegador (Ctrl+F5)

---

## 🎯 Próximos Pasos (Fase 4)

### Historial de CSRs
1. `GET /api/csr/historial` con paginación
2. `GET /api/csr/{id}` con detalle
3. Filtros y búsqueda (CN, fecha, algoritmo)
4. Seguridad: cada usuario solo ve su historial
5. Frontend: tabla de historial con recarga automática
6. Tests de integración

---

## 📞 Resumen Técnico

**Generación:** CSR PKCS#10 con BouncyCastle 1.85 (RSA 2048/4096, EC secp256r1/secp384r1)  
**Cifrado de clave:** AES-256, PKCS#8, PBKDF2 + salt  
**SANs:** Opcionales; DNS/IP auto-detectados, prefijos `DNS:`/`IP:` opcionales  
**Persistencia:** `csr_historial` (sin clave privada)  
**Salida:** PEM descargable (.csr y .key cifrada)

**Estado Actual:** Listo para Fase 4 (Historial de CSRs)

---

## 🎉 ¡Felicidades!

**Fase 3 completada exitosamente.** El backend genera CSRs reales con BouncyCastle, cifra las claves privadas y registra el historial. El siguiente paso es exponer el historial con paginación en la Fase 4.

Para más detalles, ver `Documentacion/FASE-3-Plan_de_Trabajo.md` y `Documentacion/FASE-3-Propuesta-Codigo.md`.

**Última actualización:** 2026-07-24