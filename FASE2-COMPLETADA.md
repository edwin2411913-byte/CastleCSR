# 🎉 FASE 2 COMPLETADA - CastleCSR Backend

**Fecha de Finalización:** 2026-07-24  
**Status:** ✅ COMPLETADO  
**Rama:** `main` (commit: 596ec18)  
**Tag:** `v1.0.0-phase2`

---

## 📋 Resumen de Implementación

La Fase 2 agrega **autenticación y autorización con JWT** usando la librería **Nimbus JOSE+JWT**. El token viaja en una **cookie HttpOnly** (no en headers manuales), lo que protege contra XSS, y la sesión es completamente **stateless**.

### ✅ Estructura del Proyecto (nuevos componentes)
```
src/main/java/com/castlecsr/
├── config/
│   └── SecurityConfig.java                (ACTUALIZADO: stateless + filtro JWT)
├── controller/
│   └── AuthController.java                (NUEVO: login, session, logout)
├── dto/
│   └── LoginRequest.java                  (NUEVO: username + password validados)
├── exception/
│   ├── InvalidTokenException.java         (NUEVO)
│   ├── ExpiredTokenException.java         (NUEVO)
│   └── GlobalExceptionHandler.java        (ACTUALIZADO: errores de auth)
└── security/                              (PAQUETE NUEVO)
    ├── JwtTokenProvider.java              (Generación/validación JWT con Nimbus)
    ├── JwtAuthenticationFilter.java       (Valida el token en cada request)
    ├── JwtAuthenticationEntryPoint.java   (Respuestas 401 en JSON)
    ├── CustomUserDetailsService.java      (Carga usuarios desde PostgreSQL)
    └── CookieUtil.java                    (Cookie HttpOnly auth_token)

src/main/resources/static/                 (FRONTEND NUEVO)
├── login.html                             (Página de login - pública)
├── index.html                             (Página principal - protegida)
├── js/app.js
└── css/styles.css
```

### 📦 Dependencias Agregadas
- **Nimbus JOSE+JWT:** 9.25.6 (firma y validación de tokens)
- **spring-boot-starter-validation:** Validación de DTOs (`@Valid`)

---

## 🔐 Diseño de Autenticación

### Token JWT (firmado con HS512)
| Claim | Valor | Ejemplo |
|-------|-------|---------|
| `sub` | Username del usuario | `"jgomez"` |
| `iat` | Fecha de emisión | timestamp |
| `exp` | Fecha de expiración (30 min) | timestamp |
| `iss` | Emisor | `"castlecsr-backend"` |
| `role` | Rol del usuario (custom) | `"ADMIN"` / `"USER"` |

### Cookie HttpOnly
| Atributo | Valor | Motivo |
|----------|-------|--------|
| Nombre | `auth_token` | — |
| `HttpOnly` | `true` | JavaScript no puede leer el token (anti-XSS) |
| `SameSite` | `Strict` | La cookie no viaja en requests cross-site (anti-CSRF) |
| `Secure` | `false` en local, `true` en prod | Configurable vía `jwt.cookie-secure` |
| `Max-Age` | 30 minutos | Igual que la expiración del JWT |

### Flujo de Login
```
1. POST /api/auth/login  {username, password}
2. AuthenticationManager verifica credenciales (BCrypt vs BD)
3. JwtTokenProvider genera token HS512 con Nimbus
4. Respuesta 200 con Set-Cookie: auth_token=... (HttpOnly)
5. El navegador envía la cookie automáticamente en cada request
6. JwtAuthenticationFilter valida el token y puebla el SecurityContext
```

### Configuración (variables de entorno)
```bash
# JWT_SECRET debe ser Base64 de al menos 64 bytes (requisito HS512)
# Generar con: openssl rand -base64 64
JWT_SECRET=<salida de openssl rand -base64 64>
```
```properties
# application-local.properties
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=1800000    # 30 minutos (default)
jwt.cookie-secure=false      # true en producción (HTTPS)
```

---

## 📡 Endpoints Implementados

### Públicos (sin autenticación)
```bash
GET  /api/health           → Health check
GET  /api/info             → Info de la aplicación
POST /api/auth/login       → Login (devuelve cookie auth_token)
GET  /login.html           → Página de login (+ /css/**, /js/**, /favicon.ico)
```

### Protegidos (requieren cookie JWT válida)
```bash
GET  /api/auth/session     → {id, username, rol} del usuario autenticado
POST /api/auth/logout      → Expira la cookie auth_token
GET  /                     → index.html (página principal)
*    (cualquier otro)      → 401 JSON si no hay token válido
```

### Ejemplos con curl
```bash
# Login (guarda la cookie en un archivo)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu_password"}'
# → 200 OK + Set-Cookie: auth_token=eyJhbGci...

# Sesión actual (usando la cookie)
curl -b cookies.txt http://localhost:8080/api/auth/session
# → {"id":1,"username":"admin","rol":"ADMIN"}

# Sin cookie → 401
curl http://localhost:8080/api/auth/session
# → {"status":401,"error":"Unauthorized","message":"..."}

# Logout (expira la cookie)
curl -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

---

## 🧪 Tests

**8 clases de test, 33 tests en total** (unitarios + integración):

| Clase | Tests | Qué verifica |
|-------|-------|--------------|
| `JwtTokenProviderTest` | 5 | Generación, claims, firma inválida, expiración, token malformado |
| `CookieUtilTest` | 6 | Atributos de la cookie, extracción del token, cookie expirada |
| `AuthControllerTest` | 6 | Login OK, credenciales inválidas, validación de request, session, logout |
| `JwtAuthenticationFilterTest` | 3 | Autenticación con token válido, request anónimo con token inválido |
| `CustomUserDetailsServiceTest` | 4 | Carga de usuario, roles, usuario inexistente |
| `GlobalExceptionHandlerTest` | 4 | Respuestas de error de autenticación |
| `AuthFlowIntegrationTest` | 4 | Flujo completo: login → cookie → endpoint protegido → logout |
| `CastlecsrBackendApplicationTests` | 1 | Contexto de Spring arranca |

```bash
# Ejecutar todos los tests
mvn test
```

---

## 🔐 Seguridad

### Implementado en Fase 2
- ✅ JWT firmado con **HS512** (Nimbus JOSE+JWT)
- ✅ Secreto de 64 bytes en `.env` (nunca en Git), decodificado de Base64
- ✅ Cookie **HttpOnly + SameSite=Strict** (mitiga XSS y CSRF)
- ✅ Sesiones **stateless** (`SessionCreationPolicy.STATELESS`)
- ✅ Mensaje genérico en login fallido (no revela si falló username o password)
- ✅ `JwtAuthenticationEntryPoint`: respuestas 401 en JSON consistente
- ✅ Validación de entrada en `LoginRequest` (`@Valid`)
- ✅ Filtro JWT antes de `UsernamePasswordAuthenticationFilter`
- ✅ Expiración de token verificada en cada request

### No Implementado (fases futuras)
- ❌ Refresh tokens
- ❌ Rate limiting / bloqueo por intentos fallidos
- ❌ Blacklist de tokens en servidor (logout solo expira la cookie)
- ❌ HTTPS/TLS (requerido en producción para `jwt.cookie-secure=true`)
- ❌ Autorización granular por rol con `@Secured` en endpoints de negocio (Fase 3+)

---

## 📊 Estado del Proyecto

### Métricas
- **Clases Java (main):** 21
- **Clases de test:** 8 (33 tests)
- **Frontend estático:** login.html, index.html, app.js, styles.css
- **Commit Fase 2:** +4,058 líneas / -681 líneas

### Checklist de Fase 2
- ✅ Librería Nimbus JOSE+JWT 9.25.6 integrada
- ✅ `JwtTokenProvider`: generación y validación HS512
- ✅ `AuthController`: login, session, logout
- ✅ `CustomUserDetailsService` conectado a PostgreSQL
- ✅ `JwtAuthenticationFilter` + `JwtAuthenticationEntryPoint`
- ✅ Cookie HttpOnly con `CookieUtil` (SameSite=Strict)
- ✅ Sesiones stateless configuradas
- ✅ Manejo de errores de token (inválido/expirado)
- ✅ Frontend de login funcional (páginas estáticas)
- ✅ 33 tests unitarios y de integración
- ✅ Documentación actualizada
- ✅ Tag `v1.0.0-phase2` creado

---

## 📝 Git

### Historial
```
596ec18 (tag: v1.0.0-phase2) chore: finalize Fase 2 - Authentication with JWT (Nimbus JOSE+JWT)
5320207 Se finaliza la fase 1
08c1196 docs: agregar dependencia dotenv-java al pom.xml en FASE-1-Plan de Trabajo.md
6b9ec3c docs: actualizar documentación para Fase 1 completada
234593d (tag: v1.0.0-phase1) chore: initial commit - Fase 1 scaffold
```

---

## 🎯 Próximos Pasos (Fase 3)

### Generación de CSRs con BouncyCastle
1. Crear `CsrController` con endpoint `POST /api/csr/generar`
2. Servicio de generación de pares de claves (RSA 2048/4096, EC secp256r1...)
3. Construcción del CSR en formato PEM con BouncyCastle
4. Soporte de Subject Alternative Names (SAN)
5. Persistir en `csr_historial` asociado al usuario autenticado
6. Validación de entrada (DN, algoritmo, tamaño de clave)
7. Tests unitarios y de integración

---

## 💡 Troubleshooting

### "The secret length must be at least 512 bits"
```bash
# El JWT_SECRET es muy corto para HS512. Generar uno nuevo:
openssl rand -base64 64
# Copiar la salida completa a .env como JWT_SECRET
```

### "Login devuelve 401 con credenciales correctas"
```bash
# Verificar que el password en BD es un hash BCrypt válido
psql -U castlecsr_user -d castlecsr -c "SELECT username, password_hash FROM usuarios;"
# El hash debe empezar con $2a$, $2b$ o $2y$
```

### "La cookie no se guarda en el navegador"
- Verificar que el frontend hace fetch con `credentials: 'include'`
- En local, `jwt.cookie-secure` debe ser `false` (sin HTTPS)
- El origin debe estar en la whitelist CORS (localhost:3000 / localhost:8080)

### "/api/auth/session devuelve 401 después de un rato"
- El token expira a los 30 minutos (`jwt.expiration-ms=1800000`)
- Hacer login de nuevo; no hay refresh tokens todavía

---

## 📞 Resumen Técnico

**Autenticación:** JWT (Nimbus JOSE+JWT 9.25.6) en cookie HttpOnly  
**Algoritmo de firma:** HS512 (secreto de 64 bytes, Base64 en `.env`)  
**Sesiones:** Stateless (sin estado en servidor)  
**Expiración:** 30 minutos  
**Frontend:** Páginas estáticas servidas por Spring Boot  

**Estado Actual:** Listo para Fase 3 (Generación de CSRs con BouncyCastle)

---

## 🎉 ¡Felicidades!

**Fase 2 completada exitosamente.** El backend cuenta con autenticación JWT completa y segura. El siguiente paso es implementar la generación de CSRs con BouncyCastle en la Fase 3.

Para más detalles, ver `Documentacion/FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md` y `Documentacion/FASE-2-Propuesta-Codigo-Autenticacion.md`.

**Última actualización:** 2026-07-24