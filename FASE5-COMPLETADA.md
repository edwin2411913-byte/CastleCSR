# ✅ FASE 5 COMPLETADA — Tests + Documentación + Refinamientos

**Fecha:** 2026-07-24

## Qué se implementó

### 1. Refinamientos de seguridad
- **Rate limiting en login** (`LoginRateLimiter`): máximo 5 intentos fallidos por IP en una ventana de 5 minutos (configurable con `castlecsr.security.login.max-attempts` y `castlecsr.security.login.window-seconds`). Al superarse devuelve **429** con header `Retry-After`. Un login exitoso limpia el contador.
- **Límite de tamaño de payload** (`RequestSizeLimitFilter`): requests a `/api/**` con body mayor a 64 KB devuelven **413** (configurable con `castlecsr.security.max-payload-bytes`).
- **Logging sin datos sensibles**: verificado que no se registran contraseñas, claves privadas ni tokens.

### 2. Optimizaciones
- **Compresión Gzip** habilitada para respuestas JSON/HTML/CSS/JS (> 1 KB).
- **Índices de BD**: `usuario_id` y `creado_en DESC` en `csr_historial` (ya definidos en la entidad, verificados).

### 3. Documentación (`docs/`)
- `API-SPEC.md` — todos los endpoints, request/response, códigos HTTP y reglas de validación.
- `SETUP-GUIDE.md` — instalación, PostgreSQL, secretos, arranque y configuración de seguridad.
- `DEVELOPMENT.md` — arquitectura, principios de seguridad, flujo de trabajo y tests.

### 4. CI/CD
- `.github/workflows/ci.yml` — GitHub Actions: build + tests (`mvn verify`) en cada push/PR a `main`, con JDK 21 y cache de Maven.

### 5. Tests nuevos
- `LoginRateLimiterTest` — 7 tests: bloqueo, ventana, aislamiento por IP, reset por éxito.
- `RequestSizeLimitFilterTest` — 3 tests: dentro del límite, 413, exclusión de rutas no-API.
- `AuthControllerTest.login_conRateLimitSuperado_devuelve429ConRetryAfter` — flujo 401 → 401 → 429.
- Suite completa (unitarios + integración con H2) pasa con `mvn test`.

## Validaciones ya cubiertas en fases anteriores (verificadas)
- CN/DN: obligatorios, longitudes máximas (Bean Validation en `CsrGenerationRequest`).
- País: código ISO 3166-1 alpha-2 validado contra `Locale.getISOCountries()`.
- SAN: formato DNS/IP validado (`CsrService.isValidSan`).
- Algoritmos: solo RSA (2048/4096) y EC (secp256r1/secp384r1).
- Contraseña: mínimo 8 caracteres + confirmación.
- Historial: paginación acotada (size 1–100) y aislamiento por usuario (404 para CSR ajenos).

## Configuración nueva (`application.properties`)

```properties
castlecsr.security.login.max-attempts=5
castlecsr.security.login.window-seconds=300
castlecsr.security.max-payload-bytes=65536
server.compression.enabled=true
```

## Cierre del proyecto
Con esta fase el plan de 5 fases queda completo: scaffold + BD, autenticación JWT, generación de CSR con BouncyCastle, historial paginado, y tests/documentación/hardening.

Pendiente manual: crear el tag `v1.0.0-phase5` (o `v1.0.0`) tras el commit de cierre.