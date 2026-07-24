# CastleCSR — Plan de Trabajo Backend

## 📋 Resumen ejecutivo

**STATUS:** ✅ **FASE 1 COMPLETADA** - Backend totalmente funcional

Este documento detalla el plan de desarrollo del backend de **CastleCSR** en **5 fases medianas**. Cada fase:
- ✅ Produce un **entregable funcional y probado**
- ✅ Se puede ejecutar y demostrar completamente
- ✅ Se integra progresivamente con el frontend existente
- ✅ Usa **GitHub** como repositorio remoto
- ✅ Se desarrolla en **IntelliJ IDEA**

**NOTA:** Fase 1 (Scaffold + BD) ya está completada. Ver `ESTADO-ACTUAL.md` para detalles.

---

## 🏗️ Estructura del proyecto Maven

```
castlecsr-backend/
├── .git/                              # Control de versiones (Git)
├── .gitignore                         # Archivos a ignorar en Git
├── pom.xml                            # Configuración Maven (dependencias)
├── README.md                          # Documentación del proyecto
├── CHANGELOG.md                       # Registro de cambios por fase
│
├── src/
│   ├── main/
│   │   ├── java/com/castlecsr/
│   │   │   ├── config/               # Configuración de Spring (Security, JPA, etc.)
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebConfig.java
│   │   │   │   └── JpaConfig.java
│   │   │   │
│   │   │   ├── controller/           # Controladores REST
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── CsrController.java
│   │   │   │   └── HealthController.java (para pruebas básicas)
│   │   │   │
│   │   │   ├── service/              # Lógica de negocio
│   │   │   │   ├── CsrService.java
│   │   │   │   ├── UsuarioService.java
│   │   │   │   └── CryptographyService.java
│   │   │   │
│   │   │   ├── repository/           # Acceso a datos (JPA)
│   │   │   │   ├── UsuarioRepository.java
│   │   │   │   └── CsrHistorialRepository.java
│   │   │   │
│   │   │   ├── model/                # Entidades JPA
│   │   │   │   ├── Usuario.java
│   │   │   │   └── CsrHistorial.java
│   │   │   │
│   │   │   ├── dto/                  # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── CsrGenerationRequest.java
│   │   │   │   ├── CsrGenerationResponse.java
│   │   │   │   ├── SessionResponse.java
│   │   │   │   └── ErrorResponse.java
│   │   │   │
│   │   │   ├── exception/            # Excepciones personalizadas
│   │   │   │   ├── AuthenticationException.java
│   │   │   │   ├── CsrGenerationException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   ├── security/             # Seguridad y autenticación
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── JwtUtils.java (si se implementa JWT después)
│   │   │   │
│   │   │   └── CastlecsrApplication.java  # Clase principal de Spring Boot
│   │   │
│   │   └── resources/
│   │       ├── application.properties      # Configuración general
│   │       ├── application-dev.properties  # Perfil de desarrollo
│   │       ├── application-prod.properties # Perfil de producción
│   │       ├── db/
│   │       │   └── migration/
│   │       │       └── V1__init_schema.sql # Script inicial de BD
│   │       └── static/                     # Frontend (index.html, login.html, etc.)
│   │
│   └── test/
│       ├── java/com/castlecsr/
│       │   ├── controller/
│       │   ├── service/
│       │   └── integration/
│       └── resources/
│           └── application-test.properties

├── docs/
│   ├── api-spec.md                  # Especificación de endpoints REST
│   ├── database-schema.md           # Documentación de tablas
│   └── setup-guide.md               # Guía de instalación y configuración

└── .github/
    └── workflows/
        └── ci.yml                   # CI/CD workflow (opcional, Fase 6+)
```

---

## 📁 Árbol de directorios detallado (Fase 1 - Básico)

```
castlecsr-backend/
├── pom.xml
├── .gitignore
├── README.md
├── src/main/java/com/castlecsr/
│   ├── CastlecsrApplication.java
│   ├── config/
│   │   └── WebConfig.java
│   ├── controller/
│   │   └── HealthController.java
│   ├── dto/
│   │   └── ErrorResponse.java
│   └── exception/
│       └── GlobalExceptionHandler.java
└── src/main/resources/
    ├── application.properties
    └── static/
        ├── index.html (frontend)
        ├── login.html
        ├── css/styles.css
        └── js/app.js
```

---

## 🚀 Fases de desarrollo

### **FASE 1: Scaffold + Conexión a BD** (Semana 1)
**Objetivo:** Proyecto Spring Boot funcional que arranca, conecta a PostgreSQL y crea las tablas.

**Tareas:**
1. Crear proyecto Maven con Spring Boot 4.1.0 (Java 21)
2. Configurar `pom.xml` con dependencias básicas:
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-security`
   - `postgresql` (driver JDBC)
3. Configurar `application.properties`:
   - Conexión a PostgreSQL (URL, usuario, contraseña)
   - Hibernate: `spring.jpa.hibernate.ddl-auto=update` (crear tablas automáticamente)
   - Puerto: `server.port=8080`
4. Crear entidades JPA: `Usuario.java`, `CsrHistorial.java`
5. Crear repositorios: `UsuarioRepository.java`, `CsrHistorialRepository.java`
6. Crear `HealthController.java` (endpoint GET `/api/health` como prueba)
7. Crear `GlobalExceptionHandler.java` (manejo centralizado de errores)
8. Inicializar repositorio Git + GitHub

**Entregable:**
- ✅ Backend arranca sin errores
- ✅ Conecta a PostgreSQL y crea tablas
- ✅ Endpoint `/api/health` devuelve `{"status":"OK"}`
- ✅ Repositorio Git con tag `v1.0.0-phase1`

**Comandos de prueba:**
```bash
mvn clean package
java -jar target/castlecsr-*.jar
curl http://localhost:8080/api/health
```

---

### **FASE 2: Autenticación (Login)** (Semana 2)
**Objetivo:** Login funcional con sesiones, sin CSR aún.

**Tareas:**
1. Implementar `SecurityConfig.java`:
   - `@EnableWebSecurity`
   - Deshabilitar CSRF (es API REST)
   - Configurar autenticación: usuario/contraseña contra BD
   - Ruta pública: `/login.html`, `/api/auth/login`
   - Rutas protegidas: `/index.html`, `/api/csr/**`, `/api/auth/session`
2. Crear `CustomUserDetailsService.java`:
   - Consulta tabla `usuarios` por username
   - Valida hash BCrypt de la contraseña
3. Crear DTOs:
   - `LoginRequest.java` (username, password)
   - `SessionResponse.java` (username, rol, timestamp)
4. Crear `AuthController.java`:
   - `POST /api/auth/login` → valida, crea sesión
   - `GET /api/auth/session` → devuelve usuario autenticado o 401
   - `POST /api/auth/logout` → invalida sesión
5. Seed de usuarios en BD (desde script SQL en `resources/db/migration/`)
6. CORS: permitir requests desde frontend local (localhost:3000 u otro puerto)
7. Tests básicos: `AuthControllerTest.java`

**Entregable:**
- ✅ Login funcional: POST `/api/auth/login` con usuario/contraseña
- ✅ Sesión creada; `GET /api/auth/session` devuelve datos del usuario
- ✅ Sin sesión válida: rutas protegidas devuelven 401/403
- ✅ Logout: POST `/api/auth/logout` invalida sesión
- ✅ Frontend integrado: `login.html` conecta a `/api/auth/login`
- ✅ Repositorio Git con tag `v1.0.0-phase2`

**Usuarios de prueba precargados:**
- `Edwin Figueroa` / (contraseña hasheada)
- `admin.demo` / `secret`

---

### **FASE 3: Generación de CSR** (Semana 3-4)
**Objetivo:** Backend genera CSR real con BouncyCastle; descarga de archivos.

**Tareas:**
1. Agregar dependencias BouncyCastle a `pom.xml`:
   - `bcprov-jdk18on:1.85`
   - `bcpkix-jdk18on:1.85`
2. Crear `CryptographyService.java`:
   - `generateRsaKeyPair(int keySize)` → genera par RSA (2048/4096)
   - `generateEcKeyPair(String curve)` → genera par EC (secp256r1/secp384r1)
   - `generateCSR(KeyPair, X500Name, List<GeneralName>)` → PKCS#10
   - `encryptPrivateKey(PrivateKey, String password)` → cifra con AES (PKCS#8)
   - `convertToPem(...)` → convierte a formato PEM
3. Crear DTOs:
   - `CsrGenerationRequest.java` (CN, O, OU, C, ST, L, SANs, keyType, keySize/curve, password)
   - `CsrGenerationResponse.java` (csr, keyEncrypted, csrId)
4. Crear `CsrService.java`:
   - Orquesta la generación completa
   - Valida datos del formulario
   - Persiste registro en `csr_historial` (sin clave privada)
   - Limpia memoria (clave privada, contraseña)
5. Crear `CsrController.java`:
   - `POST /api/csr/generar` → genera y devuelve CSR + key cifrada
   - Validación: usuario autenticado, datos completos, contraseña segura
6. Response multipart o JSON con base64 encoding de archivos
7. Tests: `CsrServiceTest.java`, `CsrControllerTest.java`
8. Verificación manual: openssl valida los CSR generados

**Entregable:**
- ✅ POST `/api/csr/generar` genera CSR real (verificable con `openssl req -text`)
- ✅ Clave privada cifrada con AES (descifrable con contraseña)
- ✅ Frontend descarga `.csr` y `.key` automáticamente
- ✅ Registro guardado en BD (sin clave privada)
- ✅ Validación de contraseña mínima (8 caracteres, confirmación)
- ✅ Manejo de errores: BouncyCastle, validación, BD
- ✅ Repositorio Git con tag `v1.0.0-phase3`

**Pruebas manuales:**
```bash
# Generar CSR (desde curl o frontend)
curl -X POST http://localhost:8080/api/csr/generar \
  -H "Content-Type: application/json" \
  -d '{"cn":"example.com","o":"Company","c":"MX",...}'

# Verificar CSR
openssl req -text -noout -in archivo.csr

# Verificar que la clave privada está cifrada
openssl pkey -in archivo.key -text  # Debe pedir contraseña
```

---

### **FASE 4: Historial de CSR** (Semana 4)
**Objetivo:** Vista de historial funcional, consultas a BD, paginación.

**Tareas:**
1. Crear `CsrHistorialRepository.java` (extends `JpaRepository`):
   - `findByUsuarioId(Long usuarioId, Pageable)` → historial paginado
   - `findByCnContaining(String cn)` → búsqueda por CN
2. Crear DTO `CsrHistorialResponse.java`:
   - id, fecha, cn, organizacion, algoritmo, tamaño/curva, san
3. Agregar a `CsrService.java`:
   - `getHistorial(usuarioAutenticado, page, size)` → devuelve Page<CsrHistorialResponse>
   - `getCsrDetails(id)` → detalles de un CSR (validar propiedad)
4. Crear endpoint en `CsrController.java`:
   - `GET /api/csr/historial?page=0&size=20` → lista paginada
   - `GET /api/csr/{id}` → detalles de un CSR específico
5. Frontend: tabla con historial, ordenamiento por fecha DESC, detalles en modal
6. Tests: `CsrHistorialRepositoryTest.java`

**Entregable:**
- ✅ GET `/api/csr/historial` devuelve lista paginada de CSR del usuario
- ✅ Ordenamiento por fecha (recientes primero)
- ✅ Seguridad: solo ve su propio historial
- ✅ Frontend muestra tabla interactiva
- ✅ Repositorio Git con tag `v1.0.0-phase4`

---

### **FASE 5: Tests + Documentación + Refinamientos** (Semana 5)
**Objetivo:** Cobertura de tests, API documentada, app lista para producción.

**Tareas:**
1. **Tests unitarios completos:**
   - `CsrServiceTest.java`: generación RSA/EC, validaciones
   - `AuthControllerTest.java`: login, logout, sesión
   - `CsrControllerTest.java`: generación, historial
   - `UsuarioServiceTest.java`: búsqueda, hash password
   - Cobertura mínima: 70% del código crítico
2. **Tests de integración:**
   - `AuthIntegrationTest.java`: login → acceso protegido → logout
   - `CsrGenerationIntegrationTest.java`: login → generar → historial → logout
   - Usar `@SpringBootTest` + base de datos en memoria (H2)
3. **Documentación:**
   - `API-SPEC.md`: endpoints, request/response, códigos HTTP
   - `SETUP-GUIDE.md`: instalación, configuración PostgreSQL, arranque
   - `DEVELOPMENT.md`: guía para contribuidores
4. **Refinamientos de seguridad:**
   - Validar tamaño máximo de payload JSON
   - Rate limiting en `/api/auth/login` (evitar fuerza bruta)
   - Logging sin filtrar datos sensibles (clave, contraseña)
   - HTTPS en producción (configurar certificado)
5. **Validaciones adicionales:**
   - CN y campos DN no vacíos, máxima longitud
   - SAN en formato correcto (DNS/IP)
   - País: código ISO 2 letras
   - Algoritmos permitidos solo RSA/EC
6. **Optimizaciones:**
   - Índices en BD: `usuario_id`, `creado_en`
   - Cache opcional: historial reciente
   - Compresión Gzip en respuestas
7. **CI/CD (opcional):**
   - GitHub Actions: build + test en cada push
   - Archivo `.github/workflows/ci.yml`

**Entregable:**
- ✅ Tests automatizados: `mvn test` pasa al 100%
- ✅ API documentada en `docs/api-spec.md`
- ✅ Guía de configuración y arranque
- ✅ Validaciones completas en todos los endpoints
- ✅ Seguridad reforzada
- ✅ Repositorio Git con tag `v1.0.0-phase5`
- ✅ README.md actualizado con instrucciones

---

## 📊 Cronograma estimado

| Fase | Duración | Inicio | Fin | Entrega |
|---|---|---|---|---|
| 1: Scaffold + BD | 1 semana | Semana 1 | Semana 1 | v1.0.0-phase1 |
| 2: Autenticación | 1 semana | Semana 2 | Semana 2 | v1.0.0-phase2 |
| 3: CSR Generation | 2 semanas | Semana 3 | Semana 4 | v1.0.0-phase3 |
| 4: Historial | 1 semana | Semana 4 | Semana 4 | v1.0.0-phase4 |
| 5: Tests + Docs | 1 semana | Semana 5 | Semana 5 | v1.0.0-phase5 |
| **Total** | **6 semanas** | — | — | — |

---

## 🔧 Configuración inicial de Git y GitHub

### 1. Crear repositorio en GitHub
```bash
# En GitHub web: New Repository → castlecsr-backend
# Descripción: Backend Spring Boot para generador de CSR
# Visibilidad: Public/Private (según prefieras)
```

### 2. Configurar repositorio local
```bash
# En la carpeta del proyecto
git init
git add .
git commit -m "chore: initial commit - Fase 1 scaffold"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/castlecsr-backend.git
git push -u origin main
```

### 3. Estructura de branches
```
main                          # rama de producción (tags: v1.0.0-phase1, etc.)
├── develop                   # rama de desarrollo (integración de features)
    ├── feature/phase1-scaffold
    ├── feature/phase2-auth
    ├── feature/phase3-csr
    ├── feature/phase4-history
    └── feature/phase5-tests
```

### 4. .gitignore recomendado
```
# Maven
target/
*.jar
*.war
*.nar
pom.xml.tag

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Secrets
application-local.properties
.env

# Logs
*.log
logs/
```

---

## 📦 Dependencias Maven (pom.xml resumido)

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.castlecsr</groupId>
  <artifactId>castlecsr-backend</artifactId>
  <version>1.0.0-SNAPSHOT</version>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
  </parent>

  <properties>
    <java.version>21</java.version>
    <bouncycastle.version>1.85</bouncycastle.version>
  </properties>

  <dependencies>
    <!-- Spring Boot -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>

    <!-- Cryptography -->
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcprov-jdk18on</artifactId>
      <version>${bouncycastle.version}</version>
    </dependency>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcpkix-jdk18on</artifactId>
      <version>${bouncycastle.version}</version>
    </dependency>

    <!-- Testing -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 🚀 Arranque rápido (después de Fase 1)

```bash
# 1. Clonar y abrir en IntelliJ
git clone https://github.com/TU_USUARIO/castlecsr-backend.git
# Abrir en IntelliJ → File → Open → castlecsr-backend

# 2. Configurar base de datos PostgreSQL
createdb castlecsr
# Ajustar application.properties: spring.datasource.url, username, password

# 3. Maven: descargar dependencias
mvn clean install

# 4. Ejecutar desde IntelliJ
# Main class: com.castlecsr.CastlecsrApplication
# O desde terminal: mvn spring-boot:run

# 5. Verificar
curl http://localhost:8080/api/health
```

---

## 📋 Checklist por fase

### ✅ Fase 1 Complete checklist
- [ ] Proyecto Maven creado, `pom.xml` configurado
- [ ] Entidades JPA: `Usuario`, `CsrHistorial`
- [ ] Repositorios: `UsuarioRepository`, `CsrHistorialRepository`
- [ ] `application.properties` con BD PostgreSQL
- [ ] Tablas creadas automáticamente en BD
- [ ] `HealthController.java` → GET `/api/health`
- [ ] `GlobalExceptionHandler.java` → manejo de errores
- [ ] Repositorio Git inicializado y pusheado a GitHub
- [ ] Tag `v1.0.0-phase1` creado
- [ ] README.md básico

### ✅ Fase 2 Complete checklist
- [ ] `SecurityConfig.java` → CORS, autenticación
- [ ] `CustomUserDetailsService.java` → valida contra BD
- [ ] `AuthController.java` → login, session, logout
- [ ] DTOs: `LoginRequest`, `SessionResponse`
- [ ] Seed de usuarios en BD
- [ ] Frontend `login.html` conectado
- [ ] Tests: `AuthControllerTest.java`
- [ ] Tag `v1.0.0-phase2` creado

### ✅ Fase 3 Complete checklist
- [ ] Dependencias BouncyCastle en `pom.xml`
- [ ] `CryptographyService.java` → RSA/EC, CSR, cifrado
- [ ] `CsrService.java` → orquestación
- [ ] `CsrController.java` → POST `/api/csr/generar`
- [ ] DTOs: `CsrGenerationRequest`, `CsrGenerationResponse`
- [ ] Validación de contraseña (8+ chars, confirmación)
- [ ] SAN format validation (DNS:/IP:)
- [ ] Descarga de `.csr` y `.key` desde frontend
- [ ] Tests: `CsrServiceTest.java`, `CsrControllerTest.java`
- [ ] Verificación manual con `openssl`
- [ ] Tag `v1.0.0-phase3` creado

### ✅ Fase 4 Complete checklist
- [ ] `CsrHistorialRepository.java` → queries paginadas
- [ ] `CsrService.getHistorial()` implementado
- [ ] `CsrController.java` → GET `/api/csr/historial`
- [ ] DTO: `CsrHistorialResponse.java`
- [ ] Seguridad: solo ve su propio historial
- [ ] Frontend tabla interactiva
- [ ] Tests: `CsrHistorialRepositoryTest.java`
- [ ] Tag `v1.0.0-phase4` creado

### ✅ Fase 5 Complete checklist
- [ ] Tests unitarios: 70% cobertura
- [ ] Tests de integración: login → generar → historial → logout
- [ ] `API-SPEC.md` documentado
- [ ] `SETUP-GUIDE.md` con instrucciones
- [ ] Validaciones completas (tamaño, formato, seguridad)
- [ ] Rate limiting en login
- [ ] Índices BD optimizados
- [ ] Logging sin datos sensibles
- [ ] `.github/workflows/ci.yml` (opcional)
- [ ] README.md completo
- [ ] Tag `v1.0.0-phase5` / `v1.0.0` creado

---

## 🔗 Integración Frontend-Backend

**Puerto backend:** `8080`
**URL base API:** `http://localhost:8080/api`

### Endpoints requeridos por frontend

| Método | Ruta | Frontend usa |
|---|---|---|
| POST | `/api/auth/login` | `login.html` - formulario login |
| GET | `/api/auth/session` | `app.js` - verificar sesión activa |
| POST | `/api/auth/logout` | `app.js` - botón cerrar sesión |
| POST | `/api/csr/generar` | `app.js` - form submit CSR |
| GET | `/api/csr/historial` | `app.js` - cargar tabla historial |

### CORS
Frontend puede estar en puerto diferente (ej. `localhost:3000`). Backend debe permitir:
```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowCredentials(true);
        }
    };
}
```

---

## 📞 Próximos pasos

1. **Crear proyecto Maven** en IntelliJ
2. **Inicializar Git** en la carpeta del proyecto
3. **Crear repositorio GitHub** y conectar
4. **Comenzar Fase 1:** estructura base + BD

¿Quieres que prepare el **pom.xml inicial** o la estructura de carpetas lista para descargar?
