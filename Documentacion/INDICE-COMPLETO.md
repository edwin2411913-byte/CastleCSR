# 📑 ÍNDICE COMPLETO — Documentación del Proyecto

## 📌 ESTADO ACTUAL DEL PROYECTO

```
✅ FASE 2 COMPLETADA — Autenticación JWT funcional
   Lee esto primero:  ESTADO-ACTUAL.md ⭐ (5 min)
   Luego esto:        GUIA-RAPIDA-SECRETOS.md (5 min)
   Detalle de Fase 2: FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md
```

---

## 📚 Documentación (carpeta `Documentacion/`)

### 1. **ESTADO-ACTUAL.md** ⭐⭐⭐ (LEE ESTO PRIMERO)
- **¿Qué es?** Estado actual del proyecto y cómo ejecutarlo
- **Contiene:** qué está implementado (Fases 1 y 2), endpoints, estructura
  del código, configuración, tests, troubleshooting, próximos pasos

### 2. **estructura-base-datos-CastleCSR.md** 🗄️
- **¿Qué es?** Propuesta y estructura de la base de datos
- **Contiene:** diagrama ER, tablas `usuarios` y `csr_historial`, DDL completo,
  seed data de prueba, consultas útiles, consideraciones de seguridad
- **Importante:** con `ddl-auto=validate`, este DDL es la fuente para crear las tablas

### 3. **FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md** 🔐 (IMPLEMENTADO)
- **¿Qué es?** Plan de trabajo de la Fase 2 (autenticación JWT con Nimbus)
- **Contiene:** diseño de claims, gestión del JWT_SECRET, cookie HttpOnly,
  arquitectura de seguridad — corresponde a lo que está implementado

### 4. **FASE-2-Propuesta-Codigo-Autenticacion.md** 💻 (REFERENCIA)
- **¿Qué es?** Propuesta de código de la Fase 2 (base de la implementación)

### 5. **FASE-2-Tests-Unitarios.md** 🧪 (REFERENCIA)
- **¿Qué es?** Diseño de los tests de la Fase 2 (33 tests implementados)

### 6. **GUIA-RAPIDA-SECRETOS.md** ⚡
- **¿Qué es?** Cómo proteger credenciales en desarrollo (5 min)
- **Contiene:** uso de `.env`, por qué NO subir secretos a Git, checklist pre-push

### 7. **PROTECCION-SECRETOS.md** 🔒 (PROFUNDIDAD)
- **¿Qué es?** Guía completa y técnica de seguridad de secretos
- **Contiene:** detalles técnicos, configuración en producción, troubleshooting

### 8. **castlecsr-plan-backend.md** 📋 (FASES 3-5)
- **¿Qué es?** Plan de arquitectura de las 5 fases
- **Contiene:** detalle de Fases 3 (CSR), 4 (historial) y 5 (tests/docs),
  cronograma, endpoints planeados

### 9. **RESUMEN-EJECUTIVO.md**
- **¿Qué es?** Visión general del proyecto para PMs/managers
- **Contiene:** estado de avance de las 5 fases, cronograma

### 10. **FASE-1-Plan de Trabajo.md** (HISTÓRICO)
- **¿Qué es?** Plan que se usó para la Fase 1 (ya completada)

### 11. **ACTUALIZACION-DOCUMENTACION.md** (HISTÓRICO)
- **¿Qué es?** Registro de las actualizaciones de documentación

---

## 📄 Documentos en la raíz del proyecto

| Archivo | Propósito |
|---------|-----------|
| `COMIENZA-AQUI.md` | Punto de entrada simple; redirige a lo demás |
| `README-PROYECTO-ACTUAL.md` | Guía rápida de inicio (instalar, configurar, ejecutar) |
| `FASE1-COMPLETADA.md` | Registro histórico de lo completado en Fase 1 |
| `HELP.md` | Ayuda generada por Spring Initializr |

---

## ⚙️ Archivos de Configuración

| Archivo | Propósito | ¿En Git? |
|---------|-----------|----------|
| `pom.xml` | Maven: Spring Boot 4.1.0, Nimbus JOSE+JWT, BouncyCastle, PostgreSQL | ✅ |
| `src/main/resources/application.properties` | Config por defecto (BD vía `${VARS}`, `ddl-auto=validate`, perfil `local` activo) | ✅ |
| `application-local.properties` | Config de desarrollo (logging DEBUG, `jwt.*`) | ❌ (plantilla: `.example`) |
| `src/main/resources/application-local.properties.example` | Plantilla de la config local | ✅ |
| `.env.example` | Plantilla de variables de entorno (sin valores reales) | ✅ |
| `.env` | Valores reales (DB_URL, DB_PASSWORD, JWT_SECRET) | ❌ NUNCA |
| `.gitignore` | Excluye `.env`, `application-local.properties`, claves, etc. | ✅ |
| `load-env.sh` / `load-env.bat` | Scripts para cargar variables de `.env` en la shell | ✅ |

---

## 📊 Cuál leer según tu rol

### 👨‍💻 Desarrollador Backend (¡COMIENZA AQUÍ!)

**Primer día (~25 minutos):**
1. `README-PROYECTO-ACTUAL.md` (raíz) ⭐ (10 min)
2. `ESTADO-ACTUAL.md` ⭐ (5 min)
3. `GUIA-RAPIDA-SECRETOS.md` (5 min)
4. **Ejecuta la app** siguiendo ESTADO-ACTUAL.md (5 min)

**Antes de codificar Fase 3:**
- `castlecsr-plan-backend.md` → Fase 3: Generación de CSR
- `FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md` (cómo funciona la auth actual)

### 👨‍✈️ Tech Lead / Arquitecto
1. `ESTADO-ACTUAL.md` (5 min)
2. `RESUMEN-EJECUTIVO.md` (10 min)
3. `castlecsr-plan-backend.md` (25 min) — especialmente Fases 3-5
4. `FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md` (arquitectura de auth)

### 🚀 DevOps / SRE
1. `ESTADO-ACTUAL.md` → "Cómo Ejecutar Ahora Mismo" (5 min)
2. `estructura-base-datos-CastleCSR.md` → DDL y usuario de BD (10 min)
3. `PROTECCION-SECRETOS.md` → Sección Producción (15 min)

### 🧪 QA / Tester
1. `ESTADO-ACTUAL.md` → "Endpoints" + "Verificar que Funciona" (10 min)
2. `FASE-2-Tests-Unitarios.md` → cobertura actual (10 min)

### 👤 Product Manager
1. `RESUMEN-EJECUTIVO.md` ⭐ (10 min)
2. `castlecsr-plan-backend.md` → cronograma (15 min)

---

## 🔍 Busca rápidamente por tema

- **Ejecutar la app ahora** → `ESTADO-ACTUAL.md` → "Cómo Ejecutar Ahora Mismo"
- **Entender la autenticación JWT** → `FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md`
- **Crear/entender la BD** → `estructura-base-datos-CastleCSR.md`
- **Seguridad de secretos** → `GUIA-RAPIDA-SECRETOS.md` (rápido) / `PROTECCION-SECRETOS.md` (profundo)
- **Qué está hecho** → `ESTADO-ACTUAL.md`
- **Próximas fases** → `castlecsr-plan-backend.md` → Fases 3, 4, 5
- **Configurar variables** → `.env.example` + `application-local.properties.example`
- **Tests** → `ESTADO-ACTUAL.md` → sección "Tests" / `FASE-2-Tests-Unitarios.md`

---

## 🚀 Próximos pasos (Fase 2 completada)

1. ✅ Commit + tag `v1.0.0-phase2` del trabajo de Fase 2
2. ✅ Leer `castlecsr-plan-backend.md` → "FASE 3: Generación de CSR"
3. ✅ Crear branch: `git checkout -b feature/phase3-csr`
4. ✅ Comenzar con `CryptographyService.java` (BouncyCastle)

---

## 📝 Resumen de Documentos Principales

| Documento | Propósito | Tiempo | Prioridad |
|-----------|-----------|--------|-----------|
| `README-PROYECTO-ACTUAL.md` (raíz) | Inicio rápido | 5 min | ⭐⭐⭐ |
| `ESTADO-ACTUAL.md` | Estado + ejecución | 5 min | ⭐⭐⭐ |
| `estructura-base-datos-CastleCSR.md` | BD + DDL | 10 min | ⭐⭐ |
| `FASE-2-Plan_de_Trabajo_Nimbus_JOSE_JWT_v2.md` | Auth JWT implementada | 20 min | ⭐⭐ |
| `GUIA-RAPIDA-SECRETOS.md` | Seguridad rápido | 5 min | ⭐⭐ |
| `castlecsr-plan-backend.md` | Fases 3-5 | 30 min | ⭐⭐ |
| `RESUMEN-EJECUTIVO.md` | Visión general | 10 min | ⭐ |
| `PROTECCION-SECRETOS.md` | Seguridad profunda | 20 min | ⭐ |
| `FASE1-COMPLETADA.md` (raíz) | Histórico Fase 1 | 5 min | (Referencia) |

---

**Última actualización:** 2026-07-24
**Versión:** 3.0 (Adaptada a Fase 2 Completada)
**Estado:** ✅ Autenticación JWT funcional — Siguiente: Fase 3 (CSR)
**Autor:** Equipo de Desarrollo CastleCSR