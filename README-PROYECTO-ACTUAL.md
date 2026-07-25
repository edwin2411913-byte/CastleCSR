# 🏰 CastleCSR Backend - Guía de Inicio Rápido

**Status:** ✅ Fase 3 Completada (Generación de CSR con BouncyCastle)  
**Última actualización:** 2026-07-24  
**Versión:** 1.0.0-SNAPSHOT

---

## ⚡ En 5 minutos

```bash
# 1. Configurar BD
psql -U postgres
CREATE USER castlecsr_user WITH PASSWORD 'castlecsr_password_123';
CREATE DATABASE castlecsr OWNER castlecsr_user;
\q

# 2. Configurar .env
cp .env.example .env
# Editar .env con tus credenciales

# 3. Ejecutar
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
# O en Windows: mvnw.cmd spring-boot:run ...

# 4. Verificar
curl http://localhost:8080/api/health
# {"status":"OK",...}

# 5. Login (Fase 2)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu_password"}'
# → 200 OK + cookie HttpOnly auth_token
```

---

## 📁 Estructura del Proyecto

```
CastleCSR/
├── README-PROYECTO-ACTUAL.md        ← Este archivo (COMIENZA AQUÍ)
├── FASE1-COMPLETADA.md               ← Resumen Fase 1 (scaffold)
├── FASE2-COMPLETADA.md               ← Resumen Fase 2 (autenticación JWT)
├── FASE3-COMPLETADA.md               ← Resumen Fase 3 (generación CSR)
├── .env.example                      ← Template de variables (en Git)
├── .env                              ← Tus valores locales (NO en Git)
├── pom.xml                           ← Maven: dependencias
│
├── Documentacion/                    ← Toda la documentación
│   ├── ESTADO-ACTUAL.md             ← ⭐ Lee esto primero
│   ├── GUIA-RAPIDA-SECRETOS.md      ← Seguridad en 5 min
│   ├── RESUMEN-EJECUTIVO.md         ← Visión general
│   ├── castlecsr-plan-backend.md    ← Plan 5 fases
│   ├── FASE-1-Plan de Trabajo.md    ← Detalles Fase 1
│   ├── FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md  ← Detalles Fase 2
│   ├── FASE-2-Propuesta-Codigo-Autenticacion.md      ← Código Fase 2
│   ├── FASE-2-Tests-Unitarios.md    ← Tests Fase 2
│   ├── FASE-3-Plan_de_Trabajo.md    ← Detalles Fase 3
│   ├── FASE-3-Propuesta-Codigo.md   ← Código Fase 3
│   └── PROTECCION-SECRETOS.md       ← Seguridad profunda
│
├── src/main/java/com/castlecsr/
│   ├── CastlecsrBackendApplication.java  ← Clase principal
│   │
│   ├── config/
│   │   ├── SecurityConfig.java           ← Spring Security (stateless + JWT)
│   │   ├── CryptographyConfig.java       ← Provider BouncyCastle (Fase 3)
│   │   └── EnvConfig.java                ← Cargar .env
│   │
│   ├── controller/
│   │   ├── HealthController.java         ← GET /api/health, /api/info
│   │   ├── AuthController.java           ← POST /api/auth/login, logout, session
│   │   └── CsrController.java            ← POST /api/csr/generar (Fase 3)
│   │
│   ├── service/
│   │   ├── CryptographyService.java      ← Claves RSA/EC, CSR PKCS#10, AES (Fase 3)
│   │   └── CsrService.java               ← Validación y orquestación CSR (Fase 3)
│   │
│   ├── security/
│   │   ├── JwtTokenProvider.java         ← Generación/validación JWT (Nimbus)
│   │   ├── JwtAuthenticationFilter.java  ← Valida token en cada request
│   │   ├── JwtAuthenticationEntryPoint.java ← Respuestas 401 en JSON
│   │   ├── CustomUserDetailsService.java ← Carga usuarios desde BD
│   │   └── CookieUtil.java               ← Cookie HttpOnly auth_token
│   │
│   ├── model/
│   │   ├── Usuario.java                  ← Entidad JPA
│   │   └── CsrHistorial.java             ← Entidad JPA
│   │
│   ├── repository/
│   │   ├── UsuarioRepository.java        ← Acceso a datos
│   │   └── CsrHistorialRepository.java   ← Acceso a datos
│   │
│   ├── dto/
│   │   ├── HealthResponse.java           ← DTOs
│   │   ├── ErrorResponse.java
│   │   ├── SessionResponse.java
│   │   ├── LoginRequest.java             ← Login (username + password)
│   │   ├── CsrGenerationRequest.java     ← Request CSR (Fase 3)
│   │   └── CsrGenerationResponse.java    ← Response CSR (Fase 3)
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java   ← Manejo de errores
│       ├── InvalidTokenException.java    ← Token JWT inválido
│       ├── ExpiredTokenException.java    ← Token JWT expirado
│       ├── CryptographyException.java    ← Errores de criptografía (Fase 3)
│       └── CsrGenerationException.java   ← Errores de generación CSR (Fase 3)
│
├── src/main/resources/
│   ├── application.properties            ← Configuración por defecto
│   ├── application-local.properties      ← Configuración desarrollo
│   └── static/                           ← Frontend (login.html, index.html, js, css)
│
├── src/test/java/com/castlecsr/          ← 12 clases de test (56 tests)
│
└── target/                               ← Artifacts compilados (gitignore)
```

---

## 🚀 Instalación y Ejecución

### Requisitos Previos

- **Java 21** o superior → [Descargar](https://www.oracle.com/java/technologies/downloads/)
- **PostgreSQL 12+** → [Descargar](https://www.postgresql.org/download/)
- **Git** → [Descargar](https://git-scm.com/)
- **Maven 3.8+** (incluido: `./mvnw`)

### Paso 1: Base de Datos

```bash
# Conectar como admin
psql -U postgres

# Ejecutar (dentro de psql):
CREATE USER castlecsr_user WITH PASSWORD 'castlecsr_password_123';
CREATE DATABASE castlecsr OWNER castlecsr_user;
GRANT ALL PRIVILEGES ON DATABASE castlecsr TO castlecsr_user;
\q
```

### Paso 2: Variables de Entorno

```bash
# Copiar template
cp .env.example .env

# Editar con tus valores
# Windows: notepad .env
# Linux/Mac: nano .env

# Contenido mínimo:
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
DB_USERNAME=castlecsr_user
DB_PASSWORD=castlecsr_password_123
JWT_SECRET=<generar con: openssl rand -base64 64>
```

> ⚠️ **JWT_SECRET** debe ser un valor Base64 de al menos 64 bytes (requisito del algoritmo HS512). Generarlo con `openssl rand -base64 64`.

### Paso 3: Ejecutar Aplicación

**Con Maven Wrapper (RECOMENDADO)**

```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Linux/Mac
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

**Con IntelliJ IDEA**

1. Click derecho en `CastlecsrBackendApplication.java`
2. Seleccionar `Run 'CastlecsrBackendApplication'`
3. Esperar a: `Started CastlecsrBackendApplication in X.XXX seconds`

**Con Maven global**

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### Paso 4: Verificar Funcionamiento

```bash
# Health check
curl http://localhost:8080/api/health

# Respuesta esperada:
# {"status":"OK","timestamp":"2026-07-24T12:00:00","version":"1.0.0-SNAPSHOT"}

# Info
curl http://localhost:8080/api/info

# Endpoint protegido sin login (debe dar 401)
curl http://localhost:8080/api/auth/session

# Login y uso de la cookie
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"tu_password"}'
curl -b cookies.txt http://localhost:8080/api/auth/session
# → {"id":1,"username":"admin","rol":"ADMIN"}
```

---

## 📝 Documentación Principal

### Para Empezar
1. 📄 **ESTADO-ACTUAL.md** - Qué está implementado ahora (5 min)
2. 📄 **GUIA-RAPIDA-SECRETOS.md** - Proteger credenciales (5 min)
3. 📄 **README-BACKEND.md** (cuando exista) - Referencia técnica

### Para Entender Todo
4. 📄 **RESUMEN-EJECUTIVO.md** - Visión general (10 min)
5. 📄 **castlecsr-plan-backend.md** - Plan 5 fases (30 min)
6. 📄 **PROTECCION-SECRETOS.md** - Seguridad profunda (20 min)

---

## ✅ Estado Actual (Fase 3)

| Componente | Status | Detalles |
|-----------|--------|----------|
| **Scaffold Maven** | ✅ | Spring Boot 4.1.0 + Java 21 |
| **Base de Datos** | ✅ | PostgreSQL conectado, 2 tablas |
| **Entidades JPA** | ✅ | Usuario, CsrHistorial |
| **Repositorios** | ✅ | UsuarioRepository, CsrHistorialRepository |
| **Controladores** | ✅ | HealthController, AuthController |
| **Autenticación JWT** | ✅ | Nimbus JOSE+JWT 9.25.6, HS512, cookie HttpOnly |
| **Login / Logout / Session** | ✅ | /api/auth/login, /api/auth/logout, /api/auth/session |
| **Seguridad** | ✅ | Spring Security stateless, BCrypt, CORS |
| **Manejo de Errores** | ✅ | GlobalExceptionHandler + errores de token |
| **Generación CSR** | ✅ | BouncyCastle 1.85: RSA/EC, PKCS#10, AES-256 (Fase 3) |
| **SANs flexibles** | ✅ | Opcionales; prefijos DNS:/IP: opcionales (auto-detección) |
| **Frontend** | ✅ | login.html + index.html con formulario de CSR |
| **Tests** | ✅ | 56 tests (unitarios + integración) |
| **Configuración** | ✅ | application.properties + .env |
| **Git** | ✅ | Tags v1.0.0-phase1, v1.0.0-phase2 |

---

## 🔒 Seguridad Configurada

```
✅ JWT firmado con HS512 (Nimbus JOSE+JWT)
✅ Cookie HttpOnly + SameSite=Strict (anti-XSS y anti-CSRF)
✅ Sesiones stateless (sin estado en servidor)
✅ Expiración de token: 30 minutos
✅ CORS: localhost:3000, localhost:8080 (con credentials)
✅ BCrypt: Hash seguro de contraseñas
✅ Spring Security: Control de acceso
✅ .gitignore: .env excluido de Git
✅ Endpoints públicos: /api/health, /api/info, /api/auth/login, login.html
✅ Endpoints protegidos: Requieren cookie JWT válida (401 JSON si no)
```

---

## 🔧 Comandos Útiles

### Maven

```bash
mvn clean install              # Limpiar e instalar
mvn compile                    # Solo compilar
mvn test                       # Ejecutar tests
mvn package                    # Crear JAR
mvn clean package -DskipTests  # JAR sin tests
mvn spring-boot:run            # Ejecutar (default profile)
mvn dependency:tree            # Ver dependencias
```

### Base de Datos

```bash
# Conectar
psql -U castlecsr_user -d castlecsr

# Ver tablas
\dt

# Ver columnas
\d usuarios
\d csr_historial

# Ejecutar SQL
SELECT * FROM usuarios;

# Salir
\q
```

### Git

```bash
git status                # Ver cambios
git log --oneline        # Ver commits
git tag -l               # Ver tags
git branch -a            # Ver ramas
git diff HEAD~1          # Ver cambios último commit
```

---

## 🐛 Solucionar Problemas

### "Cannot establish a connection to the database"

```bash
# Verificar PostgreSQL ejecutándose
psql -U postgres -c "SELECT version();"

# Verificar BD existe
psql -U postgres -l | grep castlecsr

# Verificar credenciales en .env
cat .env | grep DB_

# Probar conexión
psql -U castlecsr_user -d castlecsr -c "SELECT 1;"
```

### "Port 8080 is already in use"

```bash
# Cambiar puerto en application.properties
server.port=8081

# O matar proceso (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# O matar proceso (Linux/Mac)
lsof -i :8080
kill -9 <PID>
```

### "BUILD FAILURE"

```bash
mvn clean
mvn compile
java -version        # Verificar Java 21
mvn --version       # Verificar Maven 3.8+
```

### "/api/health devuelve 401"

**Verificar:**
- `SecurityConfig.java` existe
- Tiene: `.requestMatchers("/api/health").permitAll()`
- Tiene: `@Configuration` y `@EnableWebSecurity`

### "The secret length must be at least 512 bits"

```bash
# El JWT_SECRET es muy corto para HS512. Generar uno nuevo:
openssl rand -base64 64
# Copiar la salida completa a .env como JWT_SECRET
```

### "Login OK pero /api/auth/session devuelve 401"

- El token expira a los 30 minutos → hacer login de nuevo
- Desde el frontend, usar `fetch(..., { credentials: 'include' })`
- Ver más casos en `FASE2-COMPLETADA.md`

---

## 📊 Endpoints Actuales

### Públicos ✅

```
GET  /api/health              → Health check (200 OK)
GET  /api/info                → Info de app (200 OK)
POST /api/auth/login          → Login, devuelve cookie auth_token ✅ Fase 2
GET  /login.html              → Página de login ✅ Fase 2
```

### Protegidos (requieren cookie JWT) ✅

```
GET  /api/auth/session        → Sesión actual {id, username, rol} ✅ Fase 2
POST /api/auth/logout         → Logout, expira la cookie ✅ Fase 2
GET  /                        → index.html (página principal) ✅ Fase 2
POST /api/csr/generar         → Generar CSR + clave cifrada ✅ Fase 3
```

> **SANs en /api/csr/generar:** opcionales. Se aceptan `ejemplo.com`, `10.0.0.1`,
> `DNS:ejemplo.com` o `IP:10.0.0.1` (los prefijos son opcionales, con auto-detección DNS/IP).

### Próximamente

```
GET  /api/csr/historial       → Listar CSRs (Fase 4)
GET  /api/csr/{id}            → Detalles CSR (Fase 4)
```

---

## 📚 Stack Técnico

| Componente | Versión | Propósito |
|-----------|---------|-----------|
| **Java** | 21 | Lenguaje principal |
| **Spring Boot** | 4.1.0 | Framework web |
| **Spring Security** | Boot 4.1.0 | Autenticación |
| **Nimbus JOSE+JWT** | 9.25.6 | Tokens JWT (HS512) |
| **Hibernate/JPA** | Boot 4.1.0 | ORM |
| **PostgreSQL** | 12+ | Base de datos |
| **BouncyCastle** | 1.85 | Criptografía CSR (RSA/EC, PKCS#10, AES) |
| **Maven** | 3.8+ | Build tool |
| **JUnit 5** | Test | Testing |

---

## 🎯 Próximas Fases

### ✅ Fase 2: Autenticación (COMPLETADA)
- Login real con JWT (Nimbus JOSE+JWT, HS512)
- Cookie HttpOnly + sesiones stateless
- CustomUserDetailsService + filtro JWT
- 33 tests de autenticación
- Ver `FASE2-COMPLETADA.md` para detalles

### ✅ Fase 3: Generación CSR (COMPLETADA)
- BouncyCastle: claves RSA/EC + CSR PKCS#10 en PEM
- Cifrado de clave privada AES-256 (PKCS#8 + PBKDF2)
- SANs opcionales, prefijos DNS:/IP: opcionales (auto-detección)
- Descarga de .csr y .key desde el frontend
- 23 tests nuevos (56 en total)
- Ver `FASE3-COMPLETADA.md` para detalles

### Fase 4: Historial CSR (1 semana)
- Queries paginadas
- Búsqueda y filtros
- Seguridad: solo su historial
- Tests de integración

### Fase 5: Tests y Documentación (1 semana)
- 70% cobertura de tests
- API completamente documentada
- Guías de desarrollo
- CI/CD configurado

---

## 👥 Estructura del Equipo

| Rol | Responsabilidades |
|-----|------------------|
| **Backend Dev** | Implementar endpoints, lógica negocio |
| **DevOps** | Configuración BD, despliegue, CI/CD |
| **QA** | Testing manual y automatizado |
| **PM** | Seguimiento de timeline y features |

---

## 🚀 Deployment

### Desarrollo

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### Producción (Próximo)

```bash
# Compilar JAR
mvn clean package -Dmaven.test.skip=true

# Ejecutar con perfil prod
java -jar target/castlecsr-backend-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/castlecsr \
  --spring.datasource.username=${DB_USER} \
  --spring.datasource.password=${DB_PASS}
```

---

## 📞 Contacto y Soporte

- **Issues:** Reportar en GitHub
- **Docs:** Ver carpeta `Documentacion/`
- **Questions:** Ver `ESTADO-ACTUAL.md` primero

---

## 📄 Licencia

Este proyecto es parte de CastleCSR. Todos los derechos reservados.

---

## 🎉 ¡Bienvenido!

Estás viendo un backend completamente funcional listo para las fases de desarrollo. 

**Próximo paso:** Lee `ESTADO-ACTUAL.md` (5 minutos) para entender qué está implementado.

**¡Buena suerte!** 🚀