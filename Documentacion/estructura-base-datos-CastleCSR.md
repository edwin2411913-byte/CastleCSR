# CastleCSR — Estructura de Base de Datos

## 1. Descripción General

CastleCSR utiliza **PostgreSQL** como motor de base de datos para almacenar información de usuarios autenticados y el historial de solicitudes de firma de certificados (CSR) generadas. El esquema está diseñado para:

- **Autenticación**: gestionar credenciales de usuarios de forma segura
- **Auditoría**: mantener un historial completo de CSR generados
- **Seguridad**: nunca almacenar claves privadas ni contraseñas de cifrado

---

## 2. Diagrama de Relación Entidad (ER)

```
┌─────────────────────────┐
│      USUARIOS           │
├─────────────────────────┤
│ id (PK)                 │
│ username (UNIQUE)       │
│ password_hash           │
│ rol                     │
│ creado_en               │
└────────────┬────────────┘
             │
             │ 1:N
             │
             ▼
┌─────────────────────────┐
│   CSR_HISTORIAL         │
├─────────────────────────┤
│ id (PK)                 │
│ usuario_id (FK)         │
│ common_name             │
│ organizacion            │
│ unidad_organizativa     │
│ pais                    │
│ provincia               │
│ localidad               │
│ san                     │
│ algoritmo               │
│ tamano_o_curva          │
│ csr_pem                 │
│ creado_en               │
└─────────────────────────┘
```

---

## 3. Descripción Detallada de Tablas

### 3.1 Tabla `usuarios`

Almacena la información de autenticación de los usuarios del sistema.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Identificador único autoincremental del usuario |
| `username` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | Nombre de usuario para login (debe ser único) |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Hash BCrypt de la contraseña de acceso |
| `rol` | `VARCHAR(20)` | `NOT NULL`, `DEFAULT 'USER'` | Rol del usuario (`USER`, `ADMIN`, etc.) |
| `creado_en` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Fecha y hora de creación de la cuenta |

**Índices:**
- `PRIMARY KEY (id)` — búsqueda por ID
- `UNIQUE (username)` — validación de unicidad y búsqueda rápida en login

---

### 3.2 Tabla `csr_historial`

Registra cada CSR generado por los usuarios, manteniendo auditoría sin almacenar datos sensibles.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Identificador único autoincremental del registro |
| `usuario_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Referencia al usuario que generó el CSR |
| `common_name` | `VARCHAR(255)` | `NOT NULL` | CN (Common Name) del certificado |
| `organizacion` | `VARCHAR(255)` | `NOT NULL` | O (Organization) del certificado |
| `unidad_organizativa` | `VARCHAR(255)` | `NULL` | OU (Organizational Unit) del certificado |
| `pais` | `VARCHAR(2)` | `NOT NULL` | C (Country) en formato ISO 3166-1 alpha-2 |
| `provincia` | `VARCHAR(255)` | `NOT NULL` | ST (State/Province) del certificado |
| `localidad` | `VARCHAR(255)` | `NOT NULL` | L (Locality) del certificado |
| `san` | `TEXT` | `NULL` | Subject Alternative Names separados por comas |
| `algoritmo` | `VARCHAR(10)` | `NOT NULL` | Tipo de clave: `RSA` o `EC` (validación en código) |
| `tamano_o_curva` | `VARCHAR(20)` | `NOT NULL` | Tamaño RSA (2048, 4096) o curva EC (secp256r1, secp384r1) |
| `csr_pem` | `TEXT` | `NOT NULL` | Contenido completo del CSR en formato PEM (público) |
| `creado_en` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Fecha y hora de generación del CSR |

**Índices:**
- `PRIMARY KEY (id)` — búsqueda por ID
- `FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE` — integridad referencial
- `idx_csr_historial_usuario_id` sobre `usuario_id` — acelera consultas de historial por usuario
- `idx_csr_historial_creado_en` sobre `creado_en DESC` — acelera ordenamiento cronológico

**Nota sobre validación del algoritmo:**
⚠️ El CHECK constraint `CHECK (algoritmo IN ('RSA', 'EC'))` **no está implementado en la base de datos**. La validación de que el algoritmo sea solo 'RSA' o 'EC' se realiza en el **código Java** de la entidad o servicio, no en BD.

---

## 4. Tipos de Datos Utilizados

| Tipo PostgreSQL | Uso en CastleCSR | Rango / Características |
|---|---|---|
| `BIGSERIAL` | IDs autoincrementales | 1 a 9,223,372,036,854,775,807 |
| `VARCHAR(n)` | Campos de texto variable | Hasta *n* caracteres |
| `TEXT` | Contenido largo (CSR, SAN) | Sin límite práctico |
| `TIMESTAMP` | Registro de fechas | Con zona horaria implícita |
| `BIGINT` | Claves foráneas | Enteros de 64 bits |

---

## 5. Relaciones entre Tablas

### Relación 1:N — Usuarios → CSR Historial

- **Un usuario** puede tener **múltiples CSR** en su historial
- **Cada CSR** pertenece a **exactamente un usuario**
- **Integridad referencial**: si se elimina un usuario, se eliminan automáticamente sus CSR (`ON DELETE CASCADE`)

```sql
ALTER TABLE csr_historial
  ADD CONSTRAINT fk_csr_historial_usuario
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;
```

---

## 6. Script de Generación de Base de Datos (DDL Completo)

### 6.1 Creación de Tablas

```sql
-- ============================================================
-- CastleCSR — Script de Creación de Base de Datos
-- ============================================================
-- Base de datos: castlecsr
-- Motor: PostgreSQL 17+
-- Fecha: Julio 2026
-- ============================================================

-- 1. Crear tabla de usuarios
CREATE TABLE usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(20)  NOT NULL DEFAULT 'USER',
    creado_en       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_usuarios_username UNIQUE (username)
);

-- 2. Crear tabla de historial de CSR
CREATE TABLE csr_historial (
    id                    BIGSERIAL PRIMARY KEY,
    usuario_id            BIGINT       NOT NULL,
    common_name           VARCHAR(255) NOT NULL,
    organizacion          VARCHAR(255) NOT NULL,
    unidad_organizativa   VARCHAR(255),
    pais                  VARCHAR(2)   NOT NULL,
    provincia             VARCHAR(255) NOT NULL,
    localidad             VARCHAR(255) NOT NULL,
    san                   TEXT,
    algoritmo             VARCHAR(10)  NOT NULL,
    tamano_o_curva        VARCHAR(20)  NOT NULL,
    csr_pem               TEXT         NOT NULL,
    creado_en             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_csr_historial_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id)
        ON DELETE CASCADE
);

-- 3. Crear índices para optimización
CREATE INDEX idx_csr_historial_usuario_id
    ON csr_historial (usuario_id);

CREATE INDEX idx_csr_historial_creado_en
    ON csr_historial (creado_en DESC);

-- 4. Mostrar resumen de creación
SELECT 
    'Base de datos CastleCSR creada exitosamente' AS estado,
    NOW() AS timestamp_creacion;
```

---

## 7. Datos Predefinidos (Seed Data)

### 7.1 Usuarios de Prueba (Ejemplo Manual)

⚠️ **IMPORTANTE:** Los usuarios de prueba **NO se cargan automáticamente** en la base de datos. Debes insertarlos manualmente si los necesitas.

**Ejemplo de SQL para crear usuarios de prueba:**

```sql
-- ============================================================
-- Inserción de Usuarios de Prueba (MANUAL - NO AUTOMÁTICO)
-- ============================================================

INSERT INTO usuarios (username, password_hash, rol) VALUES
    ('Edwin Figueroa', '$2y$10$Tq4MER.7h7KeGgOM5BiouORjwQjc9LUxSjAtjT2pUoWx7ZBbbHFxq', 'USER'),
    ('admin.demo', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MrKtEtVwUOG0jkNRA5j2yYvMGh8lQGa', 'ADMIN');
```

### 7.2 Tabla de Referencia de Credenciales de Prueba

| username | password | rol | notas |
|----------|----------|-----|-------|
| `Edwin Figueroa` | `(hash BCrypt $2y$)` | `USER` | Usuario de prueba estándar |
| `admin.demo` | `secret` | `ADMIN` | Usuario administrador de prueba |

**Notas de seguridad:**
- ⚠️ El hash `$2a$10$N9qo8uLOickgx2ZMRZoMy.MrKtEtVwUOG0jkNRA5j2yYvMGh8lQGa` corresponde a `secret` (valor de demostración)
- ⚠️ **Estos usuarios NO se crean automáticamente**, debes ejecutar el SQL anterior manualmente si los necesitas
- ⚠️ **ANTES de producción**, crea usuarios reales con contraseñas seguras y hashes generados con BCrypt
- El prefijo `$2y$` es una variante de BCrypt compatible con Spring Security

---

## 8. Script de Generación de Datos de Prueba (DML)

### 8.1 Insertar Datos de Ejemplo de CSR

```sql
-- ============================================================
-- Datos de Ejemplo: Historial de CSR
-- ============================================================

-- Ejemplo 1: CSR con RSA 2048
INSERT INTO csr_historial 
(usuario_id, common_name, organizacion, unidad_organizativa, pais, provincia, localidad, san, algoritmo, tamano_o_curva, csr_pem, creado_en)
VALUES
(
    1,
    'www.ejemplo.com',
    'Ejemplo Corporativo S.A.',
    'Infraestructura',
    'MX',
    'Jalisco',
    'Guadalajara',
    'DNS:www.ejemplo.com,DNS:ejemplo.com,IP:192.168.1.100',
    'RSA',
    '2048',
    '-----BEGIN CERTIFICATE REQUEST-----
MIICpjCCAY4CAQAwZTELMAkGA1UEBhMCTVgxEDAOBgNVBAgMB0phbGlzY28xFDAS
BgNVBAcMC0d1YWRhbGFqYXJhMR4wHAYDVQQKDBVFamVtcGxvIENvcnBvcmF0aXZv
IFMuQS4xGDAWBgNVBAMMD3d3dy5lamVtcGxvLmNvbTCCASIwDQYJKoZIhvcNAQEB
BQADggEPADCCAQoCggEBALfB...
-----END CERTIFICATE REQUEST-----',
    NOW() - INTERVAL '7 days'
);

-- Ejemplo 2: CSR con EC secp256r1
INSERT INTO csr_historial 
(usuario_id, common_name, organizacion, unidad_organizativa, pais, provincia, localidad, san, algoritmo, tamano_o_curva, csr_pem, creado_en)
VALUES
(
    1,
    'api.ejemplo.com',
    'Ejemplo Corporativo S.A.',
    'Desarrollo',
    'MX',
    'Jalisco',
    'Guadalajara',
    'DNS:api.ejemplo.com,DNS:staging-api.ejemplo.com',
    'EC',
    'secp256r1',
    '-----BEGIN CERTIFICATE REQUEST-----
MIIBZDCCARoCCQDZ...
-----END CERTIFICATE REQUEST-----',
    NOW() - INTERVAL '3 days'
);

-- Ejemplo 3: CSR sin SAN
INSERT INTO csr_historial 
(usuario_id, common_name, organizacion, pais, provincia, localidad, algoritmo, tamano_o_curva, csr_pem, creado_en)
VALUES
(
    2,
    'admin.interno',
    'Administración',
    'MX',
    'Ciudad de México',
    'Ciudad de México',
    'RSA',
    '4096',
    '-----BEGIN CERTIFICATE REQUEST-----
MIIDZzCCAk8CAQAwZzELMAkGA1UEBhMCTVgxGDAWBgNVBAgMD0NpXnVkYWQgZGUg
-----END CERTIFICATE REQUEST-----',
    NOW() - INTERVAL '1 day'
);

-- Verificar datos insertados
SELECT COUNT(*) as total_csrs FROM csr_historial;
```

---

## 9. Consultas Útiles

### 9.1 Ver Todos los Usuarios

```sql
SELECT id, username, rol, creado_en
FROM usuarios
ORDER BY creado_en DESC;
```

### 9.2 Ver Historial Completo de un Usuario

```sql
SELECT 
    id,
    common_name,
    organizacion,
    algoritmo,
    tamano_o_curva,
    creado_en
FROM csr_historial
WHERE usuario_id = 1
ORDER BY creado_en DESC;
```

### 9.3 Ver CSR Generados en los Últimos 7 Días

```sql
SELECT 
    u.username,
    c.common_name,
    c.organizacion,
    c.algoritmo,
    c.creado_en
FROM csr_historial c
JOIN usuarios u ON c.usuario_id = u.id
WHERE c.creado_en >= NOW() - INTERVAL '7 days'
ORDER BY c.creado_en DESC;
```

### 9.4 Contar CSR por Algoritmo

```sql
SELECT 
    algoritmo,
    COUNT(*) as cantidad
FROM csr_historial
GROUP BY algoritmo
ORDER BY cantidad DESC;
```

### 9.5 Ver CSR sin SAN

```sql
SELECT 
    common_name,
    organizacion,
    creado_en
FROM csr_historial
WHERE san IS NULL
ORDER BY creado_en DESC;
```

---

## 10. Procedimientos para Mantenimiento

### 10.1 Limpiar Datos de Prueba

```sql
-- Eliminar todos los CSR de prueba
DELETE FROM csr_historial
WHERE usuario_id IN (1, 2);

-- Verificar eliminación
SELECT COUNT(*) as csrs_restantes FROM csr_historial;
```

### 10.2 Resetear Secuencias de Auto-Increment

```sql
-- Resetear secuencia de usuarios
ALTER SEQUENCE usuarios_id_seq RESTART WITH 1;

-- Resetear secuencia de CSR historial
ALTER SEQUENCE csr_historial_id_seq RESTART WITH 1;
```

### 10.3 Obtener Estadísticas de Base de Datos

```sql
SELECT 
    (SELECT COUNT(*) FROM usuarios) as total_usuarios,
    (SELECT COUNT(*) FROM csr_historial) as total_csrs,
    (SELECT COUNT(DISTINCT usuario_id) FROM csr_historial) as usuarios_con_csrs;
```

---

## 11. Consideraciones de Seguridad

### 11.1 Datos NO Almacenados Intencionalmente

Para garantizar seguridad, las siguientes entidades **nunca** se guardan en la base de datos:

- ❌ Clave privada (en texto plano o cifrada)
- ❌ Contraseña de protección de la clave privada
- ❌ Información sensible de auditoría de login fallidos

### 11.2 Protección de Datos de Usuario

- Las contraseñas se almacenan solo como **hash BCrypt**
- Los hashes BCrypt son **unidireccionales** y resistentes a ataques de fuerza bruta
- La cascada `ON DELETE CASCADE` asegura que eliminar un usuario elimine su historial

### 11.3 Cumplimiento de Estándares

- Diseño coherente con principios de **menor privilegio**
- Auditoría mediante historial de acciones (creación de CSR)
- Sin almacenamiento de credenciales o claves criptográficas

---

## 12. Parámetros de Conexión Recomendados

### 12.1 Configuración PostgreSQL (`application.properties`)

```properties
# Conexión a PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/castlecsr
spring.datasource.username=castlecsr_user
spring.datasource.password=password_segura_aqui
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate / JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Pool de conexiones
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
```

### 12.2 Usuario de Base de Datos (Para PostgreSQL)

```sql
-- Crear usuario no-superusuario para la aplicación
CREATE USER castlecsr_user WITH PASSWORD 'password_segura_aqui';

-- Otorgar permisos solo a la base de datos castlecsr
GRANT CONNECT ON DATABASE castlecsr TO castlecsr_user;
GRANT USAGE ON SCHEMA public TO castlecsr_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO castlecsr_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO castlecsr_user;
```

---

## 13. Validación Post-Creación

Ejecuta este script para verificar que todo se creó correctamente:

```sql
-- ============================================================
-- Script de Validación Post-Creación
-- ============================================================

-- 1. Verificar que las tablas existen
\dt

-- 2. Verificar estructura de la tabla usuarios
\d usuarios

-- 3. Verificar estructura de la tabla csr_historial
\d csr_historial

-- 4. Verificar índices
SELECT indexname, tablename 
FROM pg_indexes 
WHERE tablename IN ('usuarios', 'csr_historial')
ORDER BY tablename, indexname;

-- 5. Verificar usuarios predefinidos
SELECT id, username, rol FROM usuarios;

-- 6. Verificar constraints
SELECT constraint_name, table_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name IN ('usuarios', 'csr_historial')
ORDER BY table_name, constraint_type;
```

---

## 14. Notas Técnicas Finales

- **Versión PostgreSQL recomendada**: 15 o superior
- **Codificación**: UTF-8 (por defecto en PostgreSQL moderno)
- **Zona horaria**: UTC (recomendado para aplicaciones distribuidas)
- **Backup**: realizar backups regulares con `pg_dump`
- **Monitoreo**: configurar alertas para tabla `csr_historial` que crece continuamente

---

**Última actualización**: Julio 2026  
**Mantenedor**: Equipo de Desarrollo CastleCSR
