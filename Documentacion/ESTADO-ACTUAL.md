# 📊 ESTADO ACTUAL — CastleCSR Backend

**Última actualización:** 2026-07-24
**Estado:** ✅ FASE 2 COMPLETADA (Autenticación JWT)
**Branch:** `main`
**Tags:** `v1.0.0-phase1` (Fase 2 pendiente de commit/tag)

---

## 🚀 ¿Qué está listo AHORA?

### ✅ Completado en Fase 1 (Scaffold + BD)

```
✅ Proyecto Maven configurado (Spring Boot 4.1.0, Java 21)
✅ PostgreSQL conectado (BD: castelCSR_DB)
✅ 2 entidades JPA (Usuario, CsrHistorial)
✅ 2 repositorios (UsuarioRepository, CsrHistorialRepository)
✅ HealthController (/api/health, /api/info)
✅ GlobalExceptionHandler
✅ Configuración de variables de entorno (.env + dotenv-java)
✅ CORS habilitado para desarrollo local
✅ BCrypt password encoder configurado
```

### ✅ Completado en Fase 2 (Autenticación JWT)

```
✅ JWT con Nimbus JOSE+JWT 9.25.6 (firma HS512, secreto Base64 de 64 bytes)
✅ JwtTokenProvider: genera y valida tokens (claims: sub, iat, exp, iss, role)
✅ Token entregado en cookie HttpOnly "auth_token" (SameSite=Strict, 30 min)
✅ CookieUtil: creación/expiración/extracción de la cookie
✅ JwtAuthenticationFilter: valida la cookie en cada request
✅ CustomUserDetailsService: carga usuarios desde BD
✅ AuthController: POST /api/auth/login, GET /api/auth/session, POST /api/auth/logout
✅ JwtAuthenticationEntryPoint: 401 JSON para API; redirección a /login.html para navegación
✅ Excepciones propias: InvalidTokenException, ExpiredTokenException
✅ Validación de entrada con Bean Validation (LoginRequest)
✅ Frontend estático: login.html (público) + index.html (protegido por cookie JWT)
✅ 33 tests (unitarios + integración) pasando con `mvnw test`
```

---

## 🎯 Estructura del Proyecto Actual

```
src/main/java/com/castlecsr/
├── CastlecsrBackendApplication.java      ← Clase principal (Spring Boot)
│
├── config/
│   ├── SecurityConfig.java                ← Filter chain, CORS, rutas públicas/protegidas
│   └── EnvConfig.java                     ← Carga de variables .env
│
├── controller/
│   ├── HealthController.java              ← Públicos: /api/health, /api/info
│   └── AuthController.java                ← /api/auth/login, /session, /logout
│
├── dto/
│   ├── HealthResponse.java                ← Respuesta de health check
│   ├── ErrorResponse.java                 ← Formato de errores
│   ├── LoginRequest.java                  ← Credenciales de login (con validación)
│   └── SessionResponse.java               ← Datos de la sesión (id, username, rol)
│
├── exception/
│   ├── GlobalExceptionHandler.java        ← Manejo centralizado de excepciones
│   ├── InvalidTokenException.java         ← Token JWT inválido/malformado
│   └── ExpiredTokenException.java         ← Token JWT expirado
│
├── model/
│   ├── Usuario.java                       ← Entidad JPA para usuarios
│   └── CsrHistorial.java                  ← Entidad JPA para historial de CSR
│
├── repository/
│   ├── UsuarioRepository.java             ← Acceso a usuarios en BD
│   └── CsrHistorialRepository.java        ← Acceso a historial CSR en BD
│
└── security/
    ├── JwtTokenProvider.java              ← Genera/valida JWT (Nimbus, HS512)
    ├── JwtAuthenticationFilter.java       ← Lee cookie y autentica el request
    ├── JwtAuthenticationEntryPoint.java   ← 401 JSON / redirect a login.html
    ├── CookieUtil.java                    ← Cookie HttpOnly "auth_token"
    └── CustomUserDetailsService.java      ← UserDetailsService contra BD

src/main/resources/static/                 ← Frontend
├── login.html                             ← Página de login (pública)
├── index.html                             ← Página principal (requiere cookie JWT)
├── css/styles.css
└── js/app.js

src/test/java/com/castlecsr/               ← 8 clases de test, 33 tests
├── CastlecsrBackendApplicationTests.java
├── controller/AuthControllerTest.java
├── exception/GlobalExceptionHandlerTest.java
├── integration/AuthFlowIntegrationTest.java
└── security/
    ├── JwtTokenProviderTest.java
    ├── JwtAuthenticationFilterTest.java
    ├── CookieUtilTest.java
    └── CustomUserDetailsServiceTest.java
```

---

## 📋 Endpoints Implementados

### ✅ Públicos (sin autenticación)

```bash
GET /api/health
  Descripción: Health check del backend
  Respuesta: {"status":"OK","timestamp":"...","version":"1.0.0-SNAPSHOT"}

GET /api/info
  Descripción: Información de la aplicación

POST /api/auth/login
  Descripción: Login. Valida credenciales contra BD y devuelve la cookie JWT.
  Body: {"username":"...","password":"..."}
  Respuesta: 200 OK + header Set-Cookie: auth_token=... (HttpOnly)
  Errores: 401 credenciales inválidas, 400 validación de campos

Recursos estáticos públicos: /login.html, /css/**, /js/**, /favicon.ico
```

### 🔒 Protegidos (requieren cookie JWT válida)

```bash
GET /api/auth/session
  Descripción: Datos del usuario autenticado
  Respuesta: {"id":1,"username":"...","rol":"USER"} o 401

POST /api/auth/logout
  Descripción: Cierra sesión (expira la cookie auth_token)
  Respuesta: 200 OK + Set-Cookie con Max-Age=0

GET /index.html  (y "/")
  Descripción: Página principal. Sin cookie válida el navegador es
  redirigido a /login.html; las llamadas API sin token reciben 401 JSON.
```

### ⏳ Planeados (fases siguientes)

```bash
POST /api/csr/generar     → Generar nuevo CSR (Fase 3)
GET  /api/csr/historial   → Listar CSRs del usuario (Fase 4)
GET  /api/csr/{id}        → Detalle de un CSR (Fase 4)
```

---

## 🗄️ Base de Datos

**BD real de desarrollo:** `castelCSR_DB` (PostgreSQL localhost:5432, usuario `postgres`).
Ver detalle completo y DDL en `estructura-base-datos-CastleCSR.md`.

⚠️ **Importante:** `spring.jpa.hibernate.ddl-auto=validate` — Hibernate **NO crea las
tablas automáticamente**. Deben crearse con el script DDL del documento de estructura
de BD antes de arrancar la aplicación.

### Tablas

**usuarios**
```sql
id              BIGSERIAL PRIMARY KEY
username        VARCHAR(50) UNIQUE NOT NULL
password_hash   VARCHAR(255) NOT NULL      -- hash BCrypt
rol             VARCHAR(20) NOT NULL DEFAULT 'USER'
creado_en       TIMESTAMP NOT NULL
```

**csr_historial**
```sql
id                  BIGSERIAL PRIMARY KEY
usuario_id          BIGINT NOT NULL FK → usuarios.id (ON DELETE CASCADE)
common_name         VARCHAR(255) NOT NULL
organizacion        VARCHAR(255) NOT NULL
unidad_organizativa VARCHAR(255)
pais                VARCHAR(2) NOT NULL
provincia           VARCHAR(255) NOT NULL
localidad           VARCHAR(255) NOT NULL
san                 TEXT
algoritmo           VARCHAR(10) NOT NULL  -- 'RSA' o 'EC' (validado en código)
tamano_o_curva      VARCHAR(20) NOT NULL  -- '2048', '4096', 'secp256r1', etc.
csr_pem             TEXT NOT NULL
creado_en           TIMESTAMP NOT NULL

-- Índices
idx_csr_historial_usuario_id   → usuario_id
idx_csr_historial_creado_en    → creado_en DESC
```

⚠️ Para poder hacer login necesitas al menos un usuario en la tabla `usuarios`
(insertado manualmente con hash BCrypt; ver sección "Seed Data" en
`estructura-base-datos-CastleCSR.md`).

---

## 🛠️ Dependencias Principales

| Dependencia | Versión | Propósito |
|-------------|---------|-----------|
| Spring Boot | 4.1.0 | Framework principal |
| Spring Data JPA | Incluido en Boot | ORM con Hibernate |
| Spring Security | Incluido en Boot | Autenticación y autorización |
| Spring Validation | Incluido en Boot | Bean Validation en DTOs |
| Nimbus JOSE+JWT | 9.25.6 | Generación y validación de JWT (HS512) |
| PostgreSQL Driver | Incluido en Boot | Conexión a BD |
| BouncyCastle | 1.85 | Criptografía (RSA, EC, CSR — Fase 3) |
| dotenv-java | 3.0.0 | Cargar variables desde .env |
| H2 | Test | BD en memoria para tests |
| JUnit 5 / MockMvc / spring-security-test | Test | Testing |

---

## 🚀 Cómo Ejecutar Ahora Mismo

### Paso 1: Preparar la Base de Datos

```bash
# Conectar a PostgreSQL
psql -U postgres

# Crear la BD (si no existe):
CREATE DATABASE "castelCSR_DB";
\q

# Crear las tablas ejecutando el DDL de estructura-base-datos-CastleCSR.md
# (ddl-auto=validate: la app NO las crea sola)

# Insertar al menos un usuario de prueba (hash BCrypt) para poder hacer login
```

### Paso 2: Configurar Variables de Entorno

```bash
# Copiar plantilla y editar con tus credenciales
cp .env.example .env

# Contenido:
DB_URL=jdbc:postgresql://localhost:5432/castelCSR_DB
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña
# JWT_SECRET debe ser Base64 de 64 bytes (HS512). Generar con:
#   openssl rand -base64 64
JWT_SECRET=resultado_del_comando_anterior
```

### Paso 3: Ejecutar la Aplicación

**Opción A: Con Maven Wrapper (recomendado)**
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```
(El perfil `local` ya está activo por defecto en `application.properties`.)

**Opción B: Desde IntelliJ IDEA**
1. Click derecho en `CastlecsrBackendApplication.java`
2. Seleccionar `Run 'CastlecsrBackendApplication'`

**Opción C: Compilar JAR y ejecutar**
```bash
mvnw.cmd clean package -DskipTests
java -jar target/castlecsr-backend-1.0.0-SNAPSHOT.jar
```

### Paso 4: Verificar que Funciona

```bash
# Health check (200 OK)
curl http://localhost:8080/api/health

# Login (guarda la cookie)
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tu_usuario","password":"tu_password"}' \
  -c cookies.txt

# Sesión con cookie (200 con datos del usuario)
curl -b cookies.txt http://localhost:8080/api/auth/session

# Sin cookie: 401 JSON
curl http://localhost:8080/api/auth/session
# {"status":401,"error":"Unauthorized","message":"Autenticación requerida"}
```

**En el navegador:** abre `http://localhost:8080/` — sin sesión te redirige a
`/login.html`; tras el login, la cookie permite ver `index.html`.

---

## ⚙️ Configuración de la Aplicación

### application.properties (default)

- **Puerto:** `8080`, contexto `/`
- **Perfil activo por defecto:** `local`
- **BD:** `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}` (desde `.env`)
- **Hibernate:** `ddl-auto=validate` (no crea tablas)

### application-local.properties (desarrollo, NO en Git)

- Logging DEBUG, SQL en consola
- `jwt.secret=${JWT_SECRET}` — Base64 de 64 bytes
- `jwt.expiration-ms=1800000` (30 min)
- `jwt.cookie-secure=false` (local sin HTTPS; en producción debe ser `true`)

Plantilla: `application-local.properties.example`

### .env (NO en Git — plantilla en .env.example)

```env
DB_URL=jdbc:postgresql://localhost:5432/castelCSR_DB
DB_USERNAME=postgres
DB_PASSWORD=***
JWT_SECRET=***   # openssl rand -base64 64
```

---

## 🔐 Seguridad Configurada

### ✅ Implementado

```
✅ Autenticación stateless con JWT (HS512) en cookie HttpOnly
✅ Cookie SameSite=Strict (mitiga CSRF) + HttpOnly (mitiga robo por XSS)
✅ Sesiones de servidor deshabilitadas (SessionCreationPolicy.STATELESS)
✅ BCryptPasswordEncoder para contraseñas
✅ Rutas públicas explícitas; todo lo demás requiere autenticación
✅ index.html protegido: solo se sirve con cookie JWT válida
✅ 401 con formato JSON consistente para la API
✅ Redirección a /login.html para navegaciones sin sesión
✅ Mensaje genérico en login fallido (no revela si falló user o password)
✅ CORS restringido a localhost:3000/8080 con credenciales
✅ CSRF de Spring deshabilitado (API stateless + SameSite=Strict)
✅ Validación de entrada en LoginRequest (Bean Validation)
```

### ❌ NO Implementado (fases siguientes)

```
❌ Rate limiting en /api/auth/login (Fase 5)
❌ Refresh tokens
❌ HTTPS/TLS (producción; requiere jwt.cookie-secure=true)
❌ Generación de CSR (Fase 3)
❌ Historial de CSR (Fase 4)
❌ Auditoría de acceso
```

---

## 🧪 Tests

```bash
mvnw.cmd test
# Tests run: 33, Failures: 0, Errors: 0
```

| Suite | Tests | Qué cubre |
|-------|-------|-----------|
| JwtTokenProviderTest | 5 | Generación/validación de JWT, expiración, firma |
| CookieUtilTest | 6 | Creación, expiración y extracción de cookie |
| CustomUserDetailsServiceTest | 4 | Carga de usuarios desde BD |
| JwtAuthenticationFilterTest | 3 | Autenticación por cookie en requests |
| AuthControllerTest | 6 | Login/session/logout, validación, errores |
| GlobalExceptionHandlerTest | 4 | Formato de errores |
| AuthFlowIntegrationTest | 4 | Flujo completo login → session → logout (H2) |
| CastlecsrBackendApplicationTests | 1 | Contexto Spring arranca |

---

## 🐛 Troubleshooting

### "Cannot establish a connection to the database"

```bash
# 1. Verificar que PostgreSQL está ejecutándose
psql -U postgres -c "SELECT version();"

# 2. Verificar credenciales en .env
# 3. Verificar que la BD existe
psql -U postgres -l | grep castelCSR_DB
```

### "Schema-validation: missing table"

Hibernate está en `ddl-auto=validate` y las tablas no existen.
Ejecuta el DDL de `estructura-base-datos-CastleCSR.md`.

### Login devuelve 401 con credenciales correctas

- Verifica que el usuario existe en la tabla `usuarios`
- El `password_hash` debe ser un hash BCrypt válido de la contraseña
- Revisa el log con `logging.level.org.springframework.security=DEBUG`

### "The signing key must be at least 512 bits" / error al firmar JWT

`JWT_SECRET` es demasiado corto o no es Base64 válido.
Genera uno correcto: `openssl rand -base64 64`

### "Port 8080 is already in use"

```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### "/api/health devuelve 401"

- `SecurityConfig.java` debe tener `.requestMatchers("/api/health").permitAll()`

---

## 📊 Métricas Actuales

| Métrica | Valor |
|---------|-------|
| Clases Java (main) | 19 |
| Clases de test | 8 |
| Tests | 33 (todos pasando) |
| Endpoints públicos | 3 (/api/health, /api/info, /api/auth/login) |
| Endpoints protegidos | 2 (/api/auth/session, /api/auth/logout) |
| Páginas frontend | 2 (login.html pública, index.html protegida) |
| Tablas BD | 2 |
| Índices BD | 2 |

---

## 🎯 Próximos Pasos (Fase 3 — Generación de CSR)

1. **Lee:** `castlecsr-plan-backend.md` → Fase 3
2. **Implementa:**
   - `CryptographyService.java` (BouncyCastle: RSA/EC, PKCS#10, cifrado de clave)
   - `CsrService.java` (orquestación + persistencia en `csr_historial`)
   - `CsrController.java` → `POST /api/csr/generar`
   - DTOs: `CsrGenerationRequest`, `CsrGenerationResponse`
3. **Tests:** `CsrServiceTest`, `CsrControllerTest`; verificación con `openssl req -text`
4. **Git:** commit de Fase 2 + tag `v1.0.0-phase2` antes de comenzar

---

## 📞 Resumen Rápido

| Aspecto | Detalles |
|---------|----------|
| **Estado** | ✅ Fase 2 completada (autenticación JWT funcional) |
| **Puerto** | 8080 |
| **BD** | PostgreSQL localhost:5432/castelCSR_DB (usuario postgres) |
| **Java** | 21+ (pom compila a 21) |
| **Spring Boot** | 4.1.0 |
| **Auth** | JWT HS512 (Nimbus) en cookie HttpOnly, 30 min |
| **Frontend** | login.html público; index.html protegido |
| **Tests** | 33 pasando |
| **Git** | main, tag v1.0.0-phase1 (phase2 pendiente de tag) |
| **Siguiente** | Fase 3: Generación de CSR |

---

**Última actualización:** 2026-07-24
**Versión:** 2.0.0
**Autor:** Equipo de Desarrollo CastleCSR