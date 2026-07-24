# 🔒 Guía Rápida — Protección de Secretos

**Estado:** ✅ El proyecto YA implementa estas medidas de seguridad.  
**Leer:** 5 minutos  
**Propósito:** Entender cómo funcionan los secretos en CastleCSR

## 🚨 El problema en 10 segundos

```
Si subes credenciales a Git → Aparecen en el historio → ¡Comprometidas para siempre!
```

---

## ✅ La solución en 3 pasos

### PASO 1️⃣ — Crear `.env` local (SIN subir a Git)

```bash
# Copiar plantilla
cp .env.example .env

# Editar con TUS valores
nano .env
```

**Archivo `.env` (NO se sube):**
```
DB_PASSWORD=MiContraseñaRealAqui
JWT_SECRET=misecreto123abc
```

### PASO 2️⃣ — Verificar que `.env` está en `.gitignore`

```bash
# Verificar
grep "\.env" .gitignore
# Debe mostrar: ".env" ✅
```

**Archivo `.gitignore` (SÍ se sube):**
```
.env
.env.local
application-local.properties
*.keystore
```

### PASO 3️⃣ — Ejecutar con perfil `local`

```bash
# Cargar variables y ejecutar
source load-env.sh
mvn spring-boot:run
```

---

## 📋 Comparativa: Qué se sube, qué NO

| Archivo | Sube a Git? | Contiene secretos? | Ejemplo |
|---------|------------|------------------|---------|
| `.env.example` | ✅ SÍ | ❌ NO (solo plantilla) | `DB_PASSWORD=tu_contraseña_aqui` |
| `.env` | ❌ NO | ✅ SÍ (valores reales) | `DB_PASSWORD=postgres123!` |
| `.gitignore` | ✅ SÍ | ❌ NO | `.env` |
| `application.properties` | ✅ SÍ | ❌ NO (valores por defecto) | `password=${DB_PASSWORD:postgres}` |
| `application-local.properties` | ✅ SÍ | ❌ NO (solo estructura) | `password=${DB_PASSWORD:postgres}` |

---

## 🎯 Flujo de datos

```
┌─────────────────────────────────────────────────────────────────┐
│ DEVELOPMENT LOCAL (Tu máquina)                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  .env (LOCAL, NO en Git)                                        │
│  ├─ DB_PASSWORD=postgres123!                                    │
│  ├─ JWT_SECRET=abc123def456                                     │
│  └─ MAIL_PASSWORD=gmail_app_password                            │
│           ↓                                                      │
│  Spring Boot (perfil=local)                                     │
│  ├─ Lee application-local.properties                            │
│  ├─ Reemplaza ${DB_PASSWORD} con valor de .env                  │
│  └─ Arranca la aplicación                                       │
│           ↓                                                      │
│  Aplicación funcionando ✅                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ REPOSITORY (GitHub)                                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  .gitignore (EXCLUYE archivos sensibles) ✅                    │
│  ├─ .env          ❌ NO se sube                                │
│  └─ *.keystore    ❌ NO se sube                                │
│           ↓                                                      │
│  .env.example (Plantilla SIN valores) ✅                       │
│  ├─ DB_PASSWORD=tu_contraseña_aqui                             │
│  └─ JWT_SECRET=tu_jwt_secret_aqui                              │
│           ↓                                                      │
│  application-local.properties (Estructura) ✅                  │
│  └─ password=${DB_PASSWORD:default}                            │
│           ↓                                                      │
│  Otro developer clona → copia .env.example → rellena → ✅      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ PRODUCTION (Servidor)                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Variables de entorno del sistema / Secrets Manager            │
│  ├─ DB_PASSWORD (desde AWS Secrets Manager)                    │
│  ├─ JWT_SECRET (desde AWS Secrets Manager)                     │
│  └─ MAIL_PASSWORD (desde AWS Secrets Manager)                  │
│           ↓                                                      │
│  application-prod.properties                                    │
│  └─ password=${DB_PASSWORD}  (sin defaults)                    │
│           ↓                                                      │
│  Aplicación funcionando en producción ✅                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Pasos detallados para principiantes

### Para el primer setup

```bash
# 1. Clonar o crear proyecto
git clone https://github.com/usuario/castlecsr-backend.git
cd castlecsr-backend

# 2. Copiar plantilla
cp .env.example .env

# 3. Abrir editor y rellenar
# Linux/Mac:
nano .env
# Windows:
notepad .env

# Rellena esto:
# DB_PASSWORD=tu_contraseña_postgres_real
# DB_USERNAME=postgres
# JWT_SECRET=abc123def456ghi789

# 4. Guardar y cerrar (Ctrl+S, Ctrl+X si es nano)

# 5. Cargar variables
source load-env.sh  # Linux/Mac
load-env.bat        # Windows

# 6. Ejecutar
mvn spring-boot:run

# ✅ ¡Listo!
```

### Verificaciones

```bash
# ¿Está .env en .gitignore?
grep "\.env" .gitignore
# Resultado: ".env" ✅

# ¿Qué sucede si intento subir .env?
git add .env
git status
# Resultado: "nothing to commit" (ignorado) ✅

# ¿Qué se sube realmente?
git status
# Resultado: application.properties, .env.example, load-env.sh ✅
#           SIN .env ✅
```

---

## ⚠️ Errores comunes

### ❌ Error 1: Subir .env accidentalmente

```bash
# Si pasó:
# 1. Verificar histórico
git log --all -- .env

# 2. Eliminar del histórico (destructivo)
git filter-branch --tree-filter 'rm -f .env' HEAD

# 3. Force push
git push --force

# 4. IMPORTANTE: Cambiar TODAS las contraseñas
# (asume que están comprometidas)
```

### ❌ Error 2: No tener .env

```bash
# Si falta .env:
# Copiar de ejemplo
cp .env.example .env

# Rellenar valores
nano .env

# Intentar de nuevo
source load-env.sh
```

### ❌ Error 3: `.env.example` con valores reales

```bash
# ❌ Nunca hagas esto:
# .env.example con: DB_PASSWORD=postgres123!

# ✅ Siempre:
# .env.example con: DB_PASSWORD=tu_contraseña_aqui
```

---

## 📚 Resumen de archivos

```
castlecsr-backend/
│
├── .env                           ❌ NO en Git (tu máquina)
│   └─ DB_PASSWORD=real_password
│   └─ JWT_SECRET=real_secret
│
├── .env.example                   ✅ EN Git (plantilla)
│   └─ DB_PASSWORD=tu_contraseña_aqui
│   └─ JWT_SECRET=tu_jwt_secret_aqui
│
├── .gitignore                     ✅ EN Git
│   └─ .env (excluir)
│   └─ *.keystore (excluir)
│   └─ *.jks (excluir)
│
├── application.properties         ✅ EN Git
│   └─ Por defecto, sin secretos
│
├── application-local.properties   ✅ EN Git
│   └─ Estructura, carga desde .env
│
├── load-env.sh                    ✅ EN Git
│   └─ Script para cargar .env
│
└── load-env.bat                   ✅ EN Git
    └─ Script para Windows
```

---

## 🔐 Reglas de oro

1. **Nunca commits secretos**
   ```
   ❌ git add .env
   ✅ git add .env.example
   ```

2. **Nunca pushes credenciales**
   ```
   ❌ git push (si .env está committeado)
   ✅ git push (solo .env.example)
   ```

3. **`.env` es para desarrollo local**
   ```
   .env ← Tu máquina
   Secrets Manager ← Producción
   ```

4. **Rota secretos regularmente**
   ```
   Cambiar password cada 6 meses
   Cambiar JWT_SECRET si está comprometido
   ```

5. **Audita quién accede a secretos**
   ```
   En producción: logs de quién accedió
   En desarrollo: solo tú eres responsable
   ```

---

## ✅ Checklist antes de hacer push

- [ ] `.env` no está en staging
  ```bash
  git status | grep ".env"
  # Resultado: nada (no aparece) ✅
  ```

- [ ] `.gitignore` incluye `.env`
  ```bash
  grep "^\.env$" .gitignore
  # Resultado: .env ✅
  ```

- [ ] `.env.example` está versionado
  ```bash
  git ls-files | grep ".env.example"
  # Resultado: .env.example ✅
  ```

- [ ] `application.properties` no tiene secretos
  ```bash
  grep "password=" application.properties
  # Resultado: password=${DB_PASSWORD:default} ✅
  # (interpolación, no el valor real)
  ```

- [ ] Puedo hacer `git diff` sin ver credenciales
  ```bash
  git diff
  # Resultado: sin contraseñas visibles ✅
  ```

---

## 🎯 Para diferentes OS

### Linux/Mac

```bash
# Cargar y ejecutar
source load-env.sh && mvn spring-boot:run

# O en dos comandos
source load-env.sh
mvn spring-boot:run
```

### Windows (CMD)

```batch
# Ejecutar script batch
load-env.bat
# Luego:
mvn spring-boot:run
```

### Windows (PowerShell)

```powershell
# Cargar .env manualmente
Get-Content .env | ForEach-Object {
    $key, $value = $_ -split '=', 2
    if ($key -and -not $key.StartsWith('#')) {
        [Environment]::SetEnvironmentVariable($key, $value)
    }
}

# Luego ejecutar
mvn spring-boot:run
```

---

## 📞 Si algo sale mal

1. **Verificar que `.env` existe y tiene valores**
   ```bash
   cat .env | head -5
   ```

2. **Verificar que está en `.gitignore`**
   ```bash
   grep "\.env" .gitignore
   ```

3. **Verificar que cargó las variables**
   ```bash
   echo $DB_PASSWORD
   # Debe mostrar tu contraseña (si está en .env)
   ```

4. **Si nada funciona, regenerar**
   ```bash
   rm .env
   cp .env.example .env
   nano .env
   # Rellenar de nuevo
   ```

---

## 🎉 ¡Listo!

Con esto, tus secretos están protegidos. Recuerda:

```
🔒 Local: .env (sin subir)
🔒 Git: .env.example (sin valores)
🔒 Producción: Secrets Manager o env vars del sistema
```

**Pregunta:** ¿Dónde están tus credenciales ahora?

✅ **Respuesta:** En tu máquina local, protegidas, sin subir a Git.

¡Seguridad garantizada! 🚀
