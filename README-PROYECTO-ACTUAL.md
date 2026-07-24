# 🏰 CastleCSR Backend - Guía de Inicio Rápido

**Status:** ✅ Fase 1 Completada  
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
```

---

## 📁 Estructura del Proyecto

```
CastleCSR/
├── README-PROYECTO-ACTUAL.md        ← Este archivo (COMIENZA AQUÍ)
├── FASE1-COMPLETADA.md               ← Estado actual detallado
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
│   └── PROTECCION-SECRETOS.md       ← Seguridad profunda
│
├── src/main/java/com/castlecsr/
│   ├── CastlecsrBackendApplication.java  ← Clase principal
│   │
│   ├── config/
│   │   ├── SecurityConfig.java           ← Spring Security
│   │   └── EnvConfig.java                ← Cargar .env
│   │
│   ├── controller/
│   │   └── HealthController.java         ← GET /api/health, /api/info
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
│   │   └── SessionResponse.java
│   │
│   └── exception/
│       └── GlobalExceptionHandler.java   ← Manejo de errores
│
├── src/main/resources/
│   ├── application.properties            ← Configuración por defecto
│   └── application-local.properties      ← Configuración desarrollo
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
JWT_SECRET=abc123def456ghi789jkl012mno345pqr
```

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

# Endpoint protegido (debe dar 401)
curl http://localhost:8080/api/csr/historial
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

## ✅ Estado Actual (Fase 1)

| Componente | Status | Detalles |
|-----------|--------|----------|
| **Scaffold Maven** | ✅ | Spring Boot 4.1.0 + Java 21 |
| **Base de Datos** | ✅ | PostgreSQL conectado, 2 tablas |
| **Entidades JPA** | ✅ | Usuario, CsrHistorial |
| **Repositorios** | ✅ | UsuarioRepository, CsrHistorialRepository |
| **Controlador** | ✅ | HealthController (/api/health, /api/info) |
| **Seguridad** | ✅ | Spring Security, BCrypt, CORS |
| **Manejo de Errores** | ✅ | GlobalExceptionHandler |
| **Configuración** | ✅ | application.properties + .env |
| **Git** | ✅ | Tag v1.0.0-phase1 |

---

## 🔒 Seguridad Configurada

```
✅ CORS: localhost:3000, localhost:8080
✅ BCrypt: Hash seguro de contraseñas
✅ Spring Security: Control de acceso
✅ .gitignore: .env excluido de Git
✅ Endpoints públicos: Solo /api/health, /api/info
✅ Endpoints protegidos: Requieren autenticación (Fase 2)
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

---

## 📊 Endpoints Actuales

### Públicos ✅

```
GET  /api/health    → Health check (200 OK)
GET  /api/info      → Info de app (200 OK)
```

### Protegidos (Próximamente)

```
POST /api/auth/login          → Login (Fase 2)
GET  /api/auth/session        → Sesión actual (Fase 2)
POST /api/auth/logout         → Logout (Fase 2)
POST /api/csr/generar         → Generar CSR (Fase 3)
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
| **Hibernate/JPA** | Boot 4.1.0 | ORM |
| **PostgreSQL** | 12+ | Base de datos |
| **BouncyCastle** | 1.85 | Criptografía |
| **Maven** | 3.8+ | Build tool |
| **JUnit 5** | Test | Testing |

---

## 🎯 Próximas Fases

### Fase 2: Autenticación (1 semana)
- Implementar login real
- JWT (JSON Web Tokens)
- CustomUserDetailsService
- Tests de autenticación

### Fase 3: Generación CSR (2 semanas)
- BouncyCastle: generar CSR
- Cifrado de claves privadas
- Descarga de archivos
- Validación de entrada

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