# CastleCSR — Guía de instalación y configuración

## Requisitos

- **Java 21** (JDK)
- **Maven 3.9+**
- **PostgreSQL 15+**
- Git

## 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/castlecsr-backend.git
cd castlecsr-backend
```

## 2. Base de datos PostgreSQL

```sql
CREATE DATABASE castlecsr;
CREATE USER castlecsr_user WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE castlecsr TO castlecsr_user;
```

El esquema se documenta en `Documentacion/estructura-base-datos-CastleCSR.md`. Hibernate valida el esquema al arrancar (`ddl-auto=validate`), por lo que las tablas deben existir previamente.

## 3. Configurar secretos

La aplicación usa el perfil `local` con variables de entorno / `.env` (ver `PROTECCION-SECRETOS.md` y `GUIA-RAPIDA-SECRETOS.md`):

```
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
DB_USERNAME=castlecsr_user
DB_PASSWORD=tu_password
JWT_SECRET=<clave de al menos 32 bytes>
```

Copia `src/main/resources/application-local.properties.example` a `application-local.properties` y ajusta los valores. **Nunca** subas `application-local.properties` ni `.env` a Git.

## 4. Compilar y ejecutar

```bash
mvn clean package
java -jar target/castlecsr-backend-1.0.0-SNAPSHOT.jar
# o en desarrollo:
mvn spring-boot:run
```

## 5. Verificar

```bash
curl http://localhost:8080/api/health
# {"status":"OK"}
```

Frontend: abre `http://localhost:8080/login.html`.

## 6. Ejecutar tests

```bash
mvn test
```

Los tests usan H2 en memoria (perfil `test`), no requieren PostgreSQL.

## Configuración de seguridad (application.properties)

| Propiedad | Default | Descripción |
|---|---|---|
| `castlecsr.security.login.max-attempts` | 5 | Intentos fallidos de login por IP antes de bloquear |
| `castlecsr.security.login.window-seconds` | 300 | Ventana del rate limiting (segundos) |
| `castlecsr.security.max-payload-bytes` | 65536 | Tamaño máximo del body en `/api/**` |
| `server.compression.enabled` | true | Compresión Gzip de respuestas |

## Producción

- Sirve la aplicación detrás de HTTPS (proxy inverso con certificado TLS, o configura `server.ssl.*`).
- Usa un `JWT_SECRET` fuerte y rotado.
- Mantén `spring.jpa.show-sql=false` y logging en `INFO` (no se registran contraseñas ni claves).