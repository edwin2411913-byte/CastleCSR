# 📋 FASE 1 — Plan de Trabajo Completo (Definitivo)

**Objetivo:** Proyecto Spring Boot funcional que arranca, conecta a PostgreSQL y crea las tablas.

**Duración estimada:** 6-7 horas (primera semana)

**Resultado final:** Backend listo con tag `v1.0.0-phase1`

---

## 📊 Resumen de tareas

| Número | Tarea | Duración | Estado |
|--------|-------|----------|--------|
| 1 | Preparar ambiente | 30 min | ⬜ |
| 2 | Crear proyecto Maven | 20 min | ⬜ |
| 3 | Configurar dependencias | 15 min | ⬜ |
| 4 | Configurar application.properties | 15 min | ⬜ |
| 5 | Crear entidades JPA | 1 hora | ⬜ |
| 6 | Crear repositorios | 30 min | ⬜ |
| 7 | Crear HealthController | 30 min | ⬜ |
| 8 | Crear GlobalExceptionHandler | 45 min | ⬜ |
| 9 | Crear SecurityConfig | 30 min | ⬜ |
| 10 | Crear DTOs | 30 min | ⬜ |
| 11 | Crear CastlecsrApplication | 20 min | ⬜ |
| 12 | Crear BD PostgreSQL | 15 min | ⬜ |
| 13 | Crear scripts helper | 20 min | ⬜ |
| 14 | Compilar y ejecutar | 1 hora | ⬜ |
| 15 | Verificar BD | 15 min | ⬜ |
| 16 | Inicializar Git + GitHub | 30 min | ⬜ |
| 17 | Pruebas finales | 30 min | ⬜ |
| **Total** | | **6-7 horas** | |

---

## ✅ PASO 1: Preparar ambiente (30 minutos)

### 1.1 Verificar requisitos previos

```bash
# Verificar Java 21
java -version
# Debe mostrar: openjdk version "21.x.x"

# Verificar Maven
mvn --version
# Debe mostrar: Apache Maven 3.8+

# Verificar PostgreSQL
psql --version
# Debe mostrar: psql 14+

# Verificar Git
git --version
# Debe mostrar: git version 2.40+
```

**Si algo falta:**
- Java: descarga de [oracle.com](https://www.oracle.com/java/technologies/downloads/)
- Maven: descarga de [maven.apache.org](https://maven.apache.org/download.cgi)
- PostgreSQL: descarga de [postgresql.org](https://www.postgresql.org/download/)
- Git: descarga de [git-scm.com](https://git-scm.com/)

### 1.2 Crear carpeta del proyecto

```bash
# Crear directorio
mkdir castlecsr-backend
cd castlecsr-backend

# Inicializar Git
git init
git config user.name "Tu Nombre"
git config user.email "tu@email.com"
```

### 1.3 Crear estructura de carpetas

```bash
# Carpetas principal
mkdir -p src/main/java/com/castlecsr
mkdir -p src/main/resources
mkdir -p src/test/java/com/castlecsr
mkdir -p docs

# Subcarpetas en src/main/java
mkdir -p src/main/java/com/castlecsr/{model,repository,controller,service,dto,exception,config,security}

# Subcarpetas en resources
mkdir -p src/main/resources/static/{css,js}
mkdir -p src/main/resources/db/migration
```

### 1.4 Crear archivo .gitignore

Crear archivo `castlecsr-backend/.gitignore`:

```
# Maven
target/
*.jar
*.war
*.nar
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Secrets
.env
.env.local
application-local.properties
application-secrets.properties
*.keystore
*.jks
*.pem
*.key

# Logs
*.log
logs/

# Build
bin/
out/
dist/
build/
```

**Verificar:**
```bash
cat .gitignore | grep ".env"
# Resultado: .env ✅
```

---

## ✅ PASO 2: Crear proyecto Maven (20 minutos)

### 2.1 Crear pom.xml

Crear archivo `castlecsr-backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.castlecsr</groupId>
    <artifactId>castlecsr-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>CastleCSR Backend</name>
    <description>Backend Spring Boot para generador de CSR</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <bouncycastle.version>1.85</bouncycastle.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- BouncyCastle (Fase 3, pero agregamos ya) -->
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

        <!-- H2 Database for testing -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 Verificar pom.xml

```bash
mvn clean install -DskipTests
# Debe descargar todas las dependencias
# Tiempo: 2-3 minutos en primera ejecución
```

**Resultado esperado:**
```
BUILD SUCCESS
```

---

## ✅ PASO 3: Configurar dependencias (15 minutos)

### 3.1 Descargar dependencias

Ya se descargaron en paso 2.2, pero si quieres verificar:

```bash
mvn dependency:tree
# Muestra todas las dependencias resueltas
```

### 3.2 Verificar que Maven ve Java 21

```bash
mvn --version
# Debe mostrar: Java version: 21.x.x
```

---

## ✅ PASO 4: Configurar application.properties (15 minutos)

### 4.1 Crear application.properties

Crear archivo `src/main/resources/application.properties`:

```properties
# ============================================================
# Server Configuration
# ============================================================
server.port=8080
server.servlet.context-path=/
spring.application.name=castlecsr-backend

# ============================================================
# Spring Profiles
# ============================================================
spring.profiles.active=default

# ============================================================
# Database Configuration
# ============================================================
spring.datasource.url=jdbc:postgresql://localhost:5432/castlecsr
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# ============================================================
# JPA / Hibernate
# ============================================================
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# ============================================================
# Logging
# ============================================================
logging.level.root=INFO
logging.level.com.castlecsr=INFO
logging.level.org.springframework.web=INFO
logging.level.org.hibernate.SQL=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# ============================================================
# Error Handling
# ============================================================
server.error.include-message=always
server.error.include-stacktrace=on-param
server.error.include-exception=false
```

### 4.2 Crear application-local.properties

Crear archivo `src/main/resources/application-local.properties`:

```properties
# ============================================================
# Local Development Profile
# ============================================================

# Database - cargar desde variables de entorno
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/castlecsr}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

# Hibernate - más verbose en desarrollo
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# Logging - más detallado
logging.level.root=INFO
logging.level.com.castlecsr=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# CORS for local development
server.servlet.context-path=/
```

### 4.3 Crear .env.example

Crear archivo `castlecsr-backend/.env.example`:

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña_postgres_aqui

# JWT (para Fase 2)
JWT_SECRET=tu_jwt_secret_aleatorio_aqui_min_32_caracteres

# Email (para futuro)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu_email@gmail.com
MAIL_PASSWORD=tu_app_password_aqui
```

### 4.4 Crear .env local

```bash
cp .env.example .env

# Editar .env con valores locales
nano .env  # Linux/Mac
# o
notepad .env  # Windows
```

Rellena con tus valores reales:
```env
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña_real
JWT_SECRET=abc123def456ghi789jkl012mno345pqr
```

---

## ✅ PASO 5: Crear entidades JPA (1 hora)

### 5.1 Crear la clase Usuario

Crear archivo `src/main/java/com/castlecsr/model/Usuario.java`:

```java
package com.castlecsr.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String rol = "USER";

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Constructores
    public Usuario() {
    }

    public Usuario(String username, String passwordHash, String rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", rol='" + rol + '\'' +
                ", creadoEn=" + creadoEn +
                '}';
    }
}
```

### 5.2 Crear la clase CsrHistorial

Crear archivo `src/main/java/com/castlecsr/model/CsrHistorial.java`:

```java
package com.castlecsr.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "csr_historial", indexes = {
        @Index(name = "idx_csr_historial_usuario_id", columnList = "usuario_id"),
        @Index(name = "idx_csr_historial_creado_en", columnList = "creado_en DESC")
})
public class CsrHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 255)
    private String commonName;

    @Column(nullable = false, length = 255)
    private String organizacion;

    @Column(length = 255)
    private String unidadOrganizativa;

    @Column(nullable = false, length = 2)
    private String pais;

    @Column(nullable = false, length = 255)
    private String provincia;

    @Column(nullable = false, length = 255)
    private String localidad;

    @Column(columnDefinition = "TEXT")
    private String san; // Separado por comas

    @Column(nullable = false, length = 10)
    private String algoritmo; // RSA o EC

    @Column(nullable = false, length = 20)
    private String tamanioOCurva; // 2048, 4096, secp256r1, etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String csrPem; // Contenido público del CSR

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    // Constructores
    public CsrHistorial() {
    }

    public CsrHistorial(Usuario usuario, String commonName, String organizacion,
                        String pais, String provincia, String localidad,
                        String algoritmo, String tamanioOCurva, String csrPem) {
        this.usuario = usuario;
        this.commonName = commonName;
        this.organizacion = organizacion;
        this.pais = pais;
        this.provincia = provincia;
        this.localidad = localidad;
        this.algoritmo = algoritmo;
        this.tamanioOCurva = tamanioOCurva;
        this.csrPem = csrPem;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getOrganizacion() {
        return organizacion;
    }

    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

    public String getUnidadOrganizativa() {
        return unidadOrganizativa;
    }

    public void setUnidadOrganizativa(String unidadOrganizativa) {
        this.unidadOrganizativa = unidadOrganizativa;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public void setAlgoritmo(String algoritmo) {
        this.algoritmo = algoritmo;
    }

    public String getTamanioOCurva() {
        return tamanioOCurva;
    }

    public void setTamanioOCurva(String tamanioOCurva) {
        this.tamanioOCurva = tamanioOCurva;
    }

    public String getCsrPem() {
        return csrPem;
    }

    public void setCsrPem(String csrPem) {
        this.csrPem = csrPem;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public String toString() {
        return "CsrHistorial{" +
                "id=" + id +
                ", commonName='" + commonName + '\'' +
                ", organizacion='" + organizacion + '\'' +
                ", algoritmo='" + algoritmo + '\'' +
                ", creadoEn=" + creadoEn +
                '}';
    }
}
```

### 5.3 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 6: Crear repositorios (30 minutos)

### 6.1 Crear UsuarioRepository

Crear archivo `src/main/java/com/castlecsr/repository/UsuarioRepository.java`:

```java
package com.castlecsr.repository;

import com.castlecsr.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Encuentra un usuario por username
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica si existe un usuario con ese username
     */
    boolean existsByUsername(String username);
}
```

### 6.2 Crear CsrHistorialRepository

Crear archivo `src/main/java/com/castlecsr/repository/CsrHistorialRepository.java`:

```java
package com.castlecsr.repository;

import com.castlecsr.model.CsrHistorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CsrHistorialRepository extends JpaRepository<CsrHistorial, Long> {

    /**
     * Encuentra todos los CSR de un usuario (paginado)
     */
    Page<CsrHistorial> findByUsuarioIdOrderByCreaddoEnDesc(Long usuarioId, Pageable pageable);

    /**
     * Encuentra todos los CSR de un usuario sin paginación
     */
    List<CsrHistorial> findByUsuarioIdOrderByCreaddoEnDesc(Long usuarioId);

    /**
     * Busca CSR por Common Name
     */
    List<CsrHistorial> findByCommonNameContainingAndUsuarioId(String commonName, Long usuarioId);
}
```

### 6.3 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 7: Crear HealthController (30 minutos)

### 7.1 Crear DTO para respuesta de Health

Crear archivo `src/main/java/com/castlecsr/dto/HealthResponse.java`:

```java
package com.castlecsr.dto;

import java.time.LocalDateTime;

public class HealthResponse {
    private String status;
    private LocalDateTime timestamp;
    private String version;

    public HealthResponse(String status) {
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.version = "1.0.0-SNAPSHOT";
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

    // Setters
    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
```

### 7.2 Crear HealthController

Crear archivo `src/main/java/com/castlecsr/controller/HealthController.java`:

```java
package com.castlecsr.controller;

import com.castlecsr.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Endpoint de health check
     * Verificar que el backend está funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse("OK");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint adicional para obtener información de la app
     */
    @GetMapping("/info")
    public ResponseEntity<HealthResponse> info() {
        HealthResponse response = new HealthResponse("CastleCSR Backend is running");
        return ResponseEntity.ok(response);
    }
}
```

### 7.3 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 8: Crear GlobalExceptionHandler (45 minutos)

### 8.1 Crear DTO para errores

Crear archivo `src/main/java/com/castlecsr/dto/ErrorResponse.java`:

```java
package com.castlecsr.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    // Constructor simple
    public ErrorResponse(int status, String error, String message) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // Constructor con path
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message);
        this.path = path;
    }

    // Constructor con errores de validación
    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(status, error, message, path);
        this.validationErrors = validationErrors;
    }

    // Getters y Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
```

### 8.2 Crear GlobalExceptionHandler

Crear archivo `src/main/java/com/castlecsr/exception/GlobalExceptionHandler.java`:

```java
package com.castlecsr.exception;

import com.castlecsr.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validación de argumentos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Los datos proporcionados no son válidos",
                request.getDescription(false).replace("uri=", ""),
                validationErrors
        );

        logger.warn("Validation error: {}", validationErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja la excepción de recurso no encontrado
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "El recurso solicitado no existe",
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones genéricas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ha ocurrido un error en el servidor",
                request.getDescription(false).replace("uri=", "")
        );

        logger.error("Unexpected error: ", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                ex.getMessage() != null ? ex.getMessage() : "Argumento inválido",
                request.getDescription(false).replace("uri=", "")
        );

        logger.warn("Illegal argument: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
```

### 8.3 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 9: Crear SecurityConfig (30 minutos)

### ⚠️ IMPORTANTE: ¿Por qué SecurityConfig aquí?

**Sin SecurityConfig:**
- Spring Security aplica configuración por defecto
- ❌ Protege TODOS los endpoints (incluyendo `/api/health`)
- ❌ `/api/health` devuelve 401 Unauthorized
- ❌ Los tests de Fase 1 fallan

**Con SecurityConfig:**
- ✅ Definimos qué endpoints son públicos
- ✅ `/api/health` devuelve 200 OK
- ✅ `/api/info` devuelve 200 OK
- ✅ Los tests pasan
- ✅ Preparado para Fase 2 (login)

### 9.1 Crear SecurityConfig

Crear archivo `src/main/java/com/castlecsr/config/SecurityConfig.java`:

```java
package com.castlecsr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configuración de seguridad HTTP
     * Define qué endpoints son públicos y cuáles protegidos
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS habilitado
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // CSRF deshabilitado (es API REST)
                .csrf(csrf -> csrf.disable())
                
                // Configuración de autorización
                .authorizeHttpRequests(authz -> authz
                        // Rutas públicas (sin autenticación)
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/info").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        
                        // Todas las demás rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                
                // Manejo de excepciones de seguridad
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Autenticación requerida\"}"
                            );
                        })
                );

        return http.build();
    }

    /**
     * Configuración CORS para desarrollo local
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bean para encriptar contraseñas con BCrypt
     * Usado en Fase 2 para login
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 9.2 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

### 9.3 Verificar que la seguridad funciona correctamente

**Conceptos clave:**

```
✅ /api/health        → Público (200 OK)
✅ /api/info          → Público (200 OK)
✅ /api/auth/login    → Público (200 OK cuando exista en Fase 2)
❌ /api/csr/historial → Protegido (401 Unauthorized sin autenticación)
❌ Otros endpoints    → Protegidos (401 Unauthorized sin autenticación)
```

---

## ✅ PASO 10: Crear DTOs adicionales (30 minutos)

### 10.1 Crear SessionResponse

Crear archivo `src/main/java/com/castlecsr/dto/SessionResponse.java`:

```java
package com.castlecsr.dto;

public class SessionResponse {
    private Long id;
    private String username;
    private String rol;

    public SessionResponse(Long id, String username, String rol) {
        this.id = id;
        this.username = username;
        this.rol = rol;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRol() {
        return rol;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
```

### 10.2 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 11: Crear la clase principal CastlecsrApplication (20 minutos)

### 11.1 Crear CastlecsrApplication

Crear archivo `src/main/java/com/castlecsr/CastlecsrApplication.java`:

```java
package com.castlecsr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CastlecsrApplication {

    public static void main(String[] args) {
        SpringApplication.run(CastlecsrApplication.class, args);
    }
}
```

### 11.2 Verificar que compila

```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS
```

---

## ✅ PASO 12: Crear base de datos PostgreSQL (15 minutos)

### 12.1 Crear usuario y base de datos (Linux/Mac)

```bash
# Conectar a PostgreSQL
sudo -u postgres psql

# Dentro de psql:
CREATE USER castlecsr_user WITH PASSWORD 'castlecsr_password_123';
CREATE DATABASE castlecsr OWNER castlecsr_user;
GRANT ALL PRIVILEGES ON DATABASE castlecsr TO castlecsr_user;

# Salir
\q
```

### 12.2 Crear usuario y base de datos (Windows)

```cmd
# Abrir PostgreSQL command line (como admin)
# O usar pgAdmin

-- SQL commands:
CREATE USER castlecsr_user WITH PASSWORD 'castlecsr_password_123';
CREATE DATABASE castlecsr OWNER castlecsr_user;
GRANT ALL PRIVILEGES ON DATABASE castlecsr TO castlecsr_user;
```

### 12.3 Actualizar .env con credenciales

```env
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
DB_USERNAME=castlecsr_user
DB_PASSWORD=castlecsr_password_123
```

### 12.4 Verificar conexión

```bash
# Linux/Mac
psql -h localhost -U castlecsr_user -d castlecsr

# Windows (en pgAdmin o SQL Shell)
# Credenciales: castlecsr_user / castlecsr_password_123
# Database: castlecsr
```

---

## ✅ PASO 13: Crear scripts helper (20 minutos)

### 13.1 Crear load-env.sh (para Linux/Mac)

Crear archivo `castlecsr-backend/load-env.sh`:

```bash
#!/bin/bash

# Cargar variables desde .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "✅ Variables de entorno cargadas desde .env"
else
    echo "❌ Archivo .env no encontrado"
    exit 1
fi
```

Hacer ejecutable:
```bash
chmod +x load-env.sh
```

### 13.2 Crear load-env.bat (para Windows)

Crear archivo `castlecsr-backend/load-env.bat`:

```batch
@echo off
REM Cargar variables desde .env

if exist .env (
    for /f "delims== tokens=1,2" %%A in (.env) do (
        if not "%%A"=="" (
            if not "%%A:~0,1%"=="#" (
                set %%A=%%B
            )
        )
    )
    echo OK - Variables de entorno cargadas desde .env
) else (
    echo ERROR - Archivo .env no encontrado
    exit /b 1
)
```

---

## ✅ PASO 14: Compilar y ejecutar (1 hora)

### 14.1 Compilar completamente

```bash
# Limpiar y compilar
mvn clean install -DskipTests

# Resultado esperado:
# BUILD SUCCESS
```

### 14.2 Ejecutar la aplicación

**Opción A: Con perfil local (RECOMENDADO)**

```bash
# Linux/Mac
source load-env.sh
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Windows
load-env.bat
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

**Opción B: Desde IntelliJ IDEA**

1. Click derecho en `CastlecsrApplication.java`
2. `Run 'CastlecsrApplication'`
3. Esperar a que arranque

**Resultado esperado en consola:**

```
Started CastlecsrApplication in X.XXX seconds (JVM running for X.XXX)
```

### 14.3 Verificar que arranca

```bash
curl http://localhost:8080/api/health

# Resultado esperado:
# {"status":"OK","timestamp":"2026-07-21T14:30:00","version":"1.0.0-SNAPSHOT"}
```

Si recibe un JSON, ¡funcionó! ✅

---

## ✅ PASO 15: Verificar que la BD se creó (15 minutos)

### 15.1 Conectar a PostgreSQL

```bash
# Linux/Mac
psql -h localhost -U castlecsr_user -d castlecsr

# Windows (pgAdmin o SQL Shell)
```

### 15.2 Verificar tablas

```sql
-- Dentro de psql:
\dt

-- Debe mostrar:
-- public | csr_historial | table
-- public | usuarios      | table
```

### 15.3 Ver estructura de tabla usuarios

```sql
\d usuarios

-- Debe mostrar las columnas:
-- id, username, password_hash, rol, creado_en
```

### 15.4 Ver estructura de tabla csr_historial

```sql
\d csr_historial

-- Debe mostrar todas las columnas
```

---

## ✅ PASO 16: Inicializar Git y GitHub (30 minutos)

### 16.1 Verificar que .gitignore está completo

```bash
git status
# Resultado: debe mostrar solo archivos que queremos subir
# NO debe mostrar: .env, *.jar, target/, .idea/
```

### 16.2 Hacer el primer commit

```bash
# Agregar todos los archivos
git add .

# Verificar qué se va a subir
git status

# Hacer commit
git commit -m "chore: initial commit - Fase 1 scaffold"
```

### 16.3 Crear repositorio en GitHub

1. Ir a [github.com](https://github.com)
2. Click en "New repository"
3. Nombre: `castlecsr-backend`
4. Descripción: "Backend Spring Boot para generador de CSR"
5. Visibilidad: Private o Public (según prefieras)
6. Click en "Create repository"

### 16.4 Conectar repositorio local con GitHub

```bash
# Agregar remote
git remote add origin https://github.com/TU_USUARIO/castlecsr-backend.git

# Renombrar rama a main si es necesario
git branch -M main

# Push inicial
git push -u origin main

# Verificar
git branch -a
# Debe mostrar: main → origin/main
```

### 16.5 Crear tag de Fase 1

```bash
# Crear tag
git tag -a v1.0.0-phase1 -m "Fase 1: Scaffold y conexión a BD"

# Push del tag
git push origin v1.0.0-phase1

# Verificar
git tag -l
# Debe mostrar: v1.0.0-phase1
```

---

## ✅ PASO 17: Pruebas finales (30 minutos)

### 17.1 Probar endpoints públicos

```bash
# Test 1: Health check (DEBE FUNCIONAR)
curl http://localhost:8080/api/health
# Respuesta esperada: {"status":"OK",...}

# Test 2: Info (DEBE FUNCIONAR)
curl http://localhost:8080/api/info
# Respuesta esperada: {"status":"CastleCSR Backend is running",...}

# Test 3: Recurso no encontrado (404)
curl http://localhost:8080/api/not-exists
# Respuesta esperada: {"status":404,"error":"Not Found",...}
```

### 17.2 Probar endpoints protegidos (deben devolver 401)

```bash
# Test 4: Endpoint protegido sin autenticación
curl http://localhost:8080/api/csr/historial
# Respuesta esperada: {"status":401,"error":"Unauthorized","message":"Autenticación requerida"}
```

### 17.3 Probar Base de Datos

```bash
# Conectar a BD
psql -h localhost -U castlecsr_user -d castlecsr

# Verificar tablas
SELECT table_name FROM information_schema.tables 
WHERE table_schema='public';

# Resultado esperado:
--      table_name
-- ─────────────────
--  usuarios
--  csr_historial
```

### 17.4 Verificar Git

```bash
# Ver log
git log --oneline

# Resultado:
# abc1234 (HEAD -> main, tag: v1.0.0-phase1) chore: initial commit - Fase 1 scaffold

# Ver tags
git tag -l

# Resultado:
# v1.0.0-phase1

# Ver remote
git remote -v

# Resultado:
# origin  https://github.com/TU_USUARIO/castlecsr-backend.git (fetch)
# origin  https://github.com/TU_USUARIO/castlecsr-backend.git (push)
```

---

## 🎯 Checklist Final de Fase 1

- [ ] ✅ Proyecto Maven compilando sin errores
- [ ] ✅ application.properties configurado
- [ ] ✅ application-local.properties configurado
- [ ] ✅ .env.example creado (sin valores)
- [ ] ✅ .env creado con valores locales
- [ ] ✅ .gitignore contiene .env
- [ ] ✅ Entidad Usuario creada
- [ ] ✅ Entidad CsrHistorial creada
- [ ] ✅ UsuarioRepository creado
- [ ] ✅ CsrHistorialRepository creado
- [ ] ✅ HealthController creado
- [ ] ✅ GlobalExceptionHandler creado
- [ ] ✅ SecurityConfig creado (endpoints públicos funcionan)
- [ ] ✅ DTOs creados (HealthResponse, ErrorResponse, SessionResponse)
- [ ] ✅ CastlecsrApplication creado
- [ ] ✅ Base de datos PostgreSQL creada
- [ ] ✅ Tablas creadas automáticamente por Hibernate
- [ ] ✅ load-env.sh y load-env.bat creados
- [ ] ✅ Aplicación arranca sin errores
- [ ] ✅ Endpoint /api/health devuelve 200 OK
- [ ] ✅ Endpoint /api/info devuelve 200 OK
- [ ] ✅ Endpoints protegidos devuelven 401 sin autenticación
- [ ] ✅ CORS configurado para desarrollo local
- [ ] ✅ Repositorio Git inicializado
- [ ] ✅ Push a GitHub realizado
- [ ] ✅ Tag v1.0.0-phase1 creado

---

## 📊 Resumen de lo logrado

```
✅ Backend Spring Boot 4.1.0 funcionando
✅ PostgreSQL conectada y funcionando
✅ Tablas creadas automáticamente por Hibernate
✅ 2 entidades JPA (Usuario, CsrHistorial)
✅ 2 repositorios (UsuarioRepository, CsrHistorialRepository)
✅ 1 controlador REST (HealthController)
✅ 1 manejador de excepciones (GlobalExceptionHandler)
✅ 1 configurador de seguridad (SecurityConfig)
✅ 3 DTOs (HealthResponse, ErrorResponse, SessionResponse)
✅ Endpoints públicos funcionan correctamente
✅ Endpoints protegidos devuelven 401 sin autenticación
✅ CORS configurado para desarrollo local
✅ Configuración de seguridad sin Bean Config issues
✅ Repositorio Git inicializado
✅ Push a GitHub realizado
✅ Tag v1.0.0-phase1 creado

Tiempo invertido: 6-7 horas
Estado: FASE 1 COMPLETADA ✅
```

---

## 🆘 Troubleshooting

### Error: "Cannot establish a connection to the database"

**Solución:**
1. Verificar que PostgreSQL está corriendo
2. Verificar credenciales en .env
3. Verificar que la BD existe:
   ```bash
   psql -l | grep castlecsr
   ```

### Error: "Compilation failure"

**Solución:**
1. Limpiar cache: `mvn clean`
2. Recompilar: `mvn compile`
3. Verificar Java 21: `java -version`

### Error: "Cannot find /api/health"

**Solución:**
1. Verificar que el servidor arrancó
2. Verificar que está en puerto 8080
3. Intentar: `curl http://localhost:8080/api/info`

### Puerto 8080 en uso

**Solución:**
```bash
# Cambiar puerto en application.properties
server.port=8081

# O matar el proceso que usa 8080
# Linux/Mac:
lsof -i :8080
kill -9 <PID>

# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Error: "No bean of type 'PasswordEncoder' found"

**Solución:**
Verificar que `SecurityConfig.java` está creado con el bean `@Bean public PasswordEncoder passwordEncoder()`

### Error: "/api/health devuelve 401"

**Solución:**
Verificar que `SecurityConfig.java` tiene la línea:
```java
.requestMatchers("/api/health").permitAll()
```

---

## 📚 Próximos pasos

**Fase 2:** Autenticación (Login)

Cuando termines Fase 1:
1. Lee el plan de Fase 2
2. Implementa `AuthController.java`
3. Implementa `CustomUserDetailsService.java`
4. Conecta frontend con login

**¡Felicidades por completar Fase 1! 🎉**

---

## 📞 Resumen de cambios clave respecto a la arquitectura

### ✅ Diferencia importante: SecurityConfig

En Fase 1, agregamos **SecurityConfig.java** que:

```
SIN SecurityConfig:
❌ /api/health → 401 Unauthorized
❌ Tests fallan
❌ Confusion en Fase 2

CON SecurityConfig (Fase 1):
✅ /api/health → 200 OK
✅ /api/info → 200 OK
✅ Tests pasan
✅ Listo para Fase 2 (solo extender)
```

**Esto es crítico porque:**
- Spring Security está en pom.xml desde el inicio
- Sin SecurityConfig, protege TODO por defecto
- Necesitamos definir qué es público ANTES de que falle

---

**Documento generado:** 21 de Julio de 2026  
**Versión:** 2.0 (Definitiva)  
**Estado:** Listo para producción  
**Incluye:** SecurityConfig integrado  

¡Buena suerte! 💪
