# 🚀 COMIENZA AQUÍ

**Status:** ✅ Fase 1 Completada - Backend funcionando  
**Fecha:** 2026-07-24

---

## En 2 pasos

### 1️⃣ Lee esto (5 minutos)
👉 **`README-PROYECTO-ACTUAL.md`** — Te explica todo en forma rápida

### 2️⃣ Ejecuta esto (5 minutos)
```bash
# Configurar BD, .env, y ejecutar
# Ver pasos exactos en README-PROYECTO-ACTUAL.md
```

---

## Si quieres más detalles

| Necesitas... | Lee esto |
|--------------|----------|
| **Ejecutar la app** | `README-PROYECTO-ACTUAL.md` |
| **Entender estado actual** | `Documentacion/ESTADO-ACTUAL.md` |
| **Seguridad (secretos)** | `Documentacion/GUIA-RAPIDA-SECRETOS.md` |
| **Todo documentado** | `Documentacion/INDICE-COMPLETO.md` |
| **Próximas fases** | `Documentacion/castlecsr-plan-backend.md` |

---

## ⚡ Inicio Rápido

```bash
# 1. Base de datos
psql -U postgres
CREATE USER castlecsr_user WITH PASSWORD 'castlecsr_password_123';
CREATE DATABASE castlecsr OWNER castlecsr_user;
\q

# 2. Variables de entorno
cp .env.example .env
# Edita .env con tus credenciales

# 3. Crear application-local.properties (REQUERIDO)
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# 4. Ejecutar
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# 5. Verificar
curl http://localhost:8080/api/health
```

---

## 📁 Documentación

Todo está en la carpeta `Documentacion/`:
- Guías de seguridad
- Plan de 5 fases
- Estado del proyecto
- Troubleshooting

---

**👉 Siguiente:** Abre `README-PROYECTO-ACTUAL.md`