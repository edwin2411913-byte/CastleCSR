# CastleCSR — Especificación de la API REST

**Base URL:** `http://localhost:8080/api`
**Autenticación:** JWT en cookie HttpOnly (`auth_token`), emitida por `/api/auth/login`.
**Formato:** JSON (`Content-Type: application/json`). Tamaño máximo de payload: **64 KB** (excederlo devuelve `413`).

## Formato de error estándar

Todos los errores devuelven un `ErrorResponse`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "path": "/api/csr/generar",
  "validationErrors": { "campo": "mensaje" }
}
```

---

## Salud

### `GET /api/health` (público)

| Código | Descripción |
|---|---|
| 200 | `{"status":"OK", ...}` |

---

## Autenticación

### `POST /api/auth/login` (público, con rate limiting)

Request:
```json
{ "username": "admin.demo", "password": "secret" }
```

| Código | Descripción |
|---|---|
| 200 | Login correcto. Devuelve cookie `auth_token` (HttpOnly) en `Set-Cookie`. |
| 400 | Body inválido (campos vacíos). |
| 401 | Credenciales inválidas (mensaje genérico, no distingue usuario/contraseña). |
| 429 | Demasiados intentos fallidos desde la misma IP (5 en 5 minutos). Incluye header `Retry-After` con los segundos de espera. |

### `GET /api/auth/session` (protegido)

| Código | Respuesta |
|---|---|
| 200 | `{ "id": 1, "username": "admin.demo", "rol": "ADMIN" }` |
| 401 | Sin sesión válida. |

### `POST /api/auth/logout` (protegido)

| Código | Descripción |
|---|---|
| 200 | Expira la cookie `auth_token`. |

---

## CSR

### `POST /api/csr/generar` (protegido)

Request:
```json
{
  "cn": "example.com",
  "o": "Mi Empresa",
  "ou": "TI",
  "c": "MX",
  "st": "CDMX",
  "l": "Ciudad de México",
  "sans": ["DNS:www.example.com", "IP:192.168.1.1"],
  "keyType": "RSA",
  "keySize": 2048,
  "password": "clave-segura",
  "passwordConfirm": "clave-segura"
}
```

Reglas de validación:
- `cn`: obligatorio, máx. 64 caracteres.
- `o`, `st`, `l`: obligatorios, máx. 255. `ou`: opcional.
- `c`: código ISO 3166-1 alpha-2 válido (ej. `MX`).
- `sans`: opcional; formatos `DNS:host`, `IP:dirección`, o sin prefijo (se infiere).
- `keyType`: `RSA` (con `keySize` 2048 o 4096) o `EC` (con `curve` `secp256r1` o `secp384r1`).
- `password`: mínimo 8 caracteres; debe coincidir con `passwordConfirm`.

Response 200:
```json
{
  "csrId": "42",
  "csr": "-----BEGIN CERTIFICATE REQUEST-----...",
  "keyEncrypted": "-----BEGIN ENCRYPTED PRIVATE KEY-----..."
}
```

La clave privada se devuelve **cifrada con AES (PKCS#8)** y **nunca se persiste** en la base de datos.

| Código | Descripción |
|---|---|
| 200 | CSR generado. |
| 400 | Validación fallida (contraseñas no coinciden, país inválido, SAN inválido, keySize/curve no permitidos). |
| 401 | Sin autenticación. |
| 413 | Payload mayor a 64 KB. |
| 500 | Error criptográfico interno (sin detalles sensibles). |

### `GET /api/csr/historial?page=0&size=20&search=cn` (protegido)

Devuelve una página (`Page<CsrHistorialResponse>`) del historial **del usuario autenticado**, ordenada por fecha descendente.

Parámetros: `page` ≥ 0 (default 0), `size` 1–100 (default 20), `search` opcional (filtro por CN, máx. 64 caracteres).

Elemento de respuesta:
```json
{ "id": 42, "cn": "example.com", "organizacion": "Mi Empresa", "algoritmo": "RSA-2048", "fecha": "2026-07-24T10:30:00" }
```

| Código | Descripción |
|---|---|
| 200 | Página de resultados. |
| 400 | Parámetros de paginación fuera de rango. |
| 401 | Sin autenticación. |

### `GET /api/csr/{id}` (protegido)

Detalles de un CSR. Devuelve `404` si no existe **o si pertenece a otro usuario** (no se revela su existencia).