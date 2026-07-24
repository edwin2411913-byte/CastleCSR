# 🔒 Protección de Secretos en CastleCSR Backend

## Resumen

Este documento explica **cómo proteger datos sensibles** (contraseñas, API keys, credenciales de BD) en CastleCSR para que **nunca se suban a Git**.

---

## 📋 Tabla de contenidos

1. [El problema](#el-problema)
2. [La solución](#la-solución)
3. [Archivos implicados](#archivos-implicados)
4. [Cómo usar en desarrollo](#cómo-usar-en-desarrollo)
5. [Cómo usar en producción](#cómo-usar-en-producción)
6. [Troubleshooting](#troubleshooting)

---

## ❌ El problema

Si escribes credenciales directamente en `application.properties` y lo subes a GitHub:

```properties
# ❌ NUNCA hagas esto:
spring.datasource.password=MiContraseñaDelPostgres123!
jwt.secret=mi_jwt_secret_super_secreto_aqui
mail.password=contraseña_de_gmail
```

**Riesgos:**
- ⚠️ Cualquiera puede ver tus credenciales en el historio de Git
- ⚠️ Alguien puede acceder a tu base de datos
- ⚠️ Posible robo de identidad y datos
- ⚠️ No hay forma de eliminar la información de Git (permanece en el historial)

**Lección:** Una contraseña comprometida = servidor comprometido.

---

## ✅ La solución

### Estrategia de tres capas

```
ARCHIVO                          CONTENIDO                    SUBIDO A GIT?
─────────────────────────────────────────────────────────────────────────
application.properties           Config por defecto           ✅ SÍ
application-local.properties     Variables de entorno         ❌ NO
.env                             Valores reales               ❌ NO
.env.example                     Plantilla (sin valores)      ✅ SÍ
.gitignore                       Reglas de exclusión          ✅ SÍ
```

### Flujo de configuración

```
1. application.properties (valores por defecto, sin secretos)
                ↓
2. application-local.properties (carga variables de entorno)
                ↓
3. .env (archivo local, NO en Git, con valores reales)
                ↓
4. Spring Boot lee todo y arranca la app
```

---

## 📂 Archivos implicados

### 1. `.gitignore` — Qué archivos excluir

```gitignore
# Secretos - NUNCA subas estos archivos
.env
.env.local
application-local.properties
application-secrets.properties
*.keystore
*.jks
*.pem
*.key
```

**Beneficio:** Git rechaza automáticamente estos archivos.

### 2. `.env.example` — Plantilla de variables

```env
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña_aqui_para_llenar_localmente
JWT_SECRET=tu_jwt_secret_aleatorio_aqui
```

**Propósito:** 
- ✅ Muestra a otros developers qué variables necesita la app
- ✅ Se sube a Git (sin valores reales)
- ✅ Cada developer copia → rellena localmente

### 3. `application-local.properties` — Carga las variables

```properties
spring.datasource.password=${DB_PASSWORD:postgres}
jwt.secret=${JWT_SECRET:default-secret}
```

**Cómo funciona:**
- `${DB_PASSWORD:postgres}` = Lee la variable `DB_PASSWORD` del entorno; si no existe, usa `postgres` como default
- Se sube a Git (muestra la estructura, no los valores)

### 4. `.env` — Valores reales (LOCAL ONLY)

```env
DB_PASSWORD=MiContraseñaRealDelPostgres123!
JWT_SECRET=abc123def456ghi789jkl012mno345pqr
```

**Muy importante:**
- ❌ **NUNCA** se sube a Git
- 🔒 Solo en tu máquina local
- 🔐 Protégelo, es sensible

---

## 🚀 Cómo usar en desarrollo

### Paso 1: Copiar plantilla

```bash
# Copiar .env.example a .env (local, sin subir)
cp .env.example .env

# Verificar que está en .gitignore
cat .gitignore | grep "\.env"
```

### Paso 2: Llenar valores reales

Edita `.env` con tus credenciales reales:

```env
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña_real_de_postgres
DB_URL=jdbc:postgresql://localhost:5432/castlecsr
JWT_SECRET=abc123def456ghi789jkl012mno345pqr
```

### Paso 3: Ejecutar con perfil local

#### Opción A: Desde terminal

```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# O en un comando
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

#### Opción B: Desde IntelliJ IDEA

1. Abre `Run` → `Edit Configurations`
2. Selecciona o crea `CastlecsrApplication`
3. En "Environment variables", agrega:
   ```
   SPRING_PROFILES_ACTIVE=local
   DB_PASSWORD=tu_contraseña_aqui
   JWT_SECRET=tu_jwt_secret_aqui
   ```
4. Clic en `Apply` → `OK`
5. Ejecuta la aplicación (▶️)

#### Opción C: Desde VS Code

En `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "CastleCSR Local",
      "type": "java",
      "name": "CastlecsrApplication",
      "request": "launch",
      "mainClass": "com.castlecsr.CastlecsrApplication",
      "args": "--spring.profiles.active=local",
      "env": {
        "SPRING_PROFILES_ACTIVE": "local",
        "DB_PASSWORD": "tu_contraseña_aqui"
      }
    }
  ]
}
```

### Paso 4: Verificar que funciona

```bash
# Si ves esto en la consola:
# "Started CastlecsrApplication in X.XXX seconds"
# ✅ Funcionó

# Si ves "Connection refused":
# ❌ Revisa credenciales en .env
```

---

## 🏢 Cómo usar en producción

### Estrategia: Variables de entorno del servidor

En lugar de `.env`, usa variables de entorno del sistema:

#### Linux/Mac

```bash
# Set en la shell o en /etc/profile
export DB_PASSWORD="contraseña_de_producción"
export JWT_SECRET="jwt_secret_de_producción"

# O al iniciar la app
java -jar target/castlecsr.jar \
  -Dspring.datasource.password="contraseña_de_producción" \
  -Djwt.secret="jwt_secret_de_producción"
```

#### Docker (recomendado para producción)

```dockerfile
FROM openjdk:21

WORKDIR /app
COPY target/castlecsr.jar .

# Variables de entorno en el container
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_URL=${DB_URL}
ENV DB_USERNAME=${DB_USERNAME}
ENV DB_PASSWORD=${DB_PASSWORD}

ENTRYPOINT ["java", "-jar", "castlecsr.jar"]
```

```bash
# Ejecutar con variables
docker run -e DB_PASSWORD="prod_password" \
           -e JWT_SECRET="prod_jwt" \
           castlecsr-backend
```

#### AWS / Azure / Google Cloud

Usa **Secrets Manager**:

- **AWS Secrets Manager:** Almacena secretos, Spring Boot los lee
- **Azure Key Vault:** Integración con Spring Cloud Azure
- **Google Secret Manager:** Integración con Spring Cloud GCP

Ejemplo (AWS):

```java
// Spring Boot carga automáticamente de AWS Secrets Manager
@Configuration
public class SecretsConfig {
    @Value("${spring.datasource.password}")  // Viene de AWS Secrets Manager
    private String dbPassword;
}
```

---

## 📋 Checklist de seguridad

### Para cada developer

- [ ] Copiaste `.env.example` a `.env`
- [ ] Llenaste `.env` con valores reales
- [ ] Verificaste que `.env` está en `.gitignore`
- [ ] NO subiste `.env` a Git
- [ ] Ejecutas con `spring.profiles.active=local`

### Para el repositorio

- [ ] `.gitignore` contiene:
  ```
  .env
  .env.local
  application-local.properties
  *.keystore
  *.jks
  *.pem
  *.key
  ```
- [ ] `.env.example` está versionado (sin valores)
- [ ] `application-local.properties` está versionado (con variables)
- [ ] `application.properties` tiene valores por defecto seguros (ej. `postgres:postgres`)

### Para producción

- [ ] No uses contraseña = variable de entorno o secrets manager
- [ ] Rotación de secretos: cambia JWT_SECRET cada 6 meses
- [ ] Auditoria: log quién accedió a qué secretos
- [ ] HTTPS obligatorio en producción

---

## 🆘 Troubleshooting

### Error: "Cannot establish a connection to the database"

**Causa:** `.env` tiene credenciales incorrectas

**Solución:**
```bash
# Verifica la contraseña en .env
cat .env | grep DB_PASSWORD

# Prueba conectar directamente
psql -U postgres -d castlecsr -h localhost
```

### Error: "Unknown property 'spring.profiles.active'"

**Causa:** No estás usando el perfil `local`

**Solución:**
```bash
# Verificar que el perfil está activo
echo $SPRING_PROFILES_ACTIVE

# Si está vacío, exportarlo
export SPRING_PROFILES_ACTIVE=local
```

### Error: "application-local.properties not found"

**Causa:** El archivo no existe o está en la ruta incorrecta

**Solución:**
```bash
# Debe estar en src/main/resources/
ls -la src/main/resources/application-local.properties

# Si no existe, créalo
touch src/main/resources/application-local.properties
```

### Se subió .env a Git accidentalmente

**ACCIÓN INMEDIATA:**
```bash
# 1. Eliminar del histórico (⚠️ destructivo)
git filter-branch --tree-filter 'rm -f .env' HEAD

# 2. Force push (solo si eres el único o con consentimiento del equipo)
git push --force

# 3. Rotar secretos: cambiar todas las contraseñas, API keys, etc.
```

---

## 📝 Ejemplo completo

### Proyecto nuevo

```bash
# 1. Clonar repo
git clone https://github.com/usuario/castlecsr-backend.git
cd castlecsr-backend

# 2. Copiar plantilla
cp .env.example .env

# 3. Editar .env con valores locales
nano .env
# DB_PASSWORD=mi_contraseña
# DB_USERNAME=postgres
# JWT_SECRET=abc123...

# 4. Verificar que está en .gitignore
cat .gitignore | grep "\.env"  # Debe mostrar ".env"

# 5. Ejecutar
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# ✅ Listo
```

### Agregar developer al equipo

```bash
# Developer nuevo clona el repo
git clone https://github.com/usuario/castlecsr-backend.git

# Ve que existe .env.example
ls -la .env.example  # ✅ Existe

# Copia y rellena
cp .env.example .env
nano .env
# Llena con sus valores locales

# Git rechaza .env automáticamente
git add .  # .env no se agrega
git status  # No muestra .env

# ✅ Listo, puede desarrollar
```

---

## 🔐 Resumen de archivos

| Archivo | Contenido | Git? | Quién ve? | Sensible? |
|---------|-----------|------|-----------|-----------|
| `.env.example` | Template sin valores | ✅ SÍ | Todos | No |
| `.env` | Valores reales | ❌ NO | Solo local | ✅ SÍ |
| `application.properties` | Config por defecto | ✅ SÍ | Todos | No |
| `application-local.properties` | Carga variables | ✅ SÍ | Todos | No |
| `application-prod.properties` | Config de prod | ⚠️ (sin secretos) | Algunos | No |
| `.gitignore` | Reglas de exclusión | ✅ SÍ | Todos | No |

---

## 📚 Más información

- [Spring Boot Profiles](https://spring.io/blog/2015/12/04/spring-boot-1-3-0-released)
- [12 Factor App - Config](https://12factor.net/config)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)

---

## ✅ Conclusión

**Regla de oro:**
```
❌ Nunca commits secretos
❌ Nunca pushes credenciales
✅ Usa .env para desarrollo
✅ Usa secrets manager para producción
✅ Rota secretos regularmente
```

**Recuerda:** Una contraseña comprometida puede destruir toda la seguridad del proyecto. Protégelas como si fuera tu propia contraseña de banco. 🏦🔐

