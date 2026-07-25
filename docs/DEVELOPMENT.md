# CastleCSR — Guía para contribuidores

## Arquitectura

Spring Boot 4.1 (Java 21), arquitectura en capas:

```
controller/   → Endpoints REST (Auth, Csr, Health)
service/      → Lógica de negocio (CsrService) y criptografía (CryptographyService, BouncyCastle)
repository/   → Spring Data JPA (PostgreSQL en runtime, H2 en tests)
security/     → JWT (Nimbus JOSE+JWT), cookie HttpOnly, rate limiting, filtros
model/        → Entidades JPA (Usuario, CsrHistorial)
dto/          → Objetos request/response con Bean Validation
exception/    → Excepciones de dominio + GlobalExceptionHandler
config/       → SecurityConfig, CryptographyConfig, EnvConfig
```

## Principios de seguridad del proyecto

1. **La clave privada nunca se persiste** — solo se devuelve cifrada (AES, PKCS#8) en la respuesta y la contraseña se sobrescribe en memoria tras usarse.
2. **Mensajes de error genéricos** — el login no revela si falló usuario o contraseña; los CSR ajenos devuelven 404, no 403.
3. **No loggear datos sensibles** — nunca registrar contraseñas, claves ni tokens.
4. **Todo endpoint valida propiedad** — el historial y los detalles siempre filtran por el usuario autenticado.

## Flujo de trabajo

1. Rama por fase/feature: `feature/phaseN-descripcion`.
2. Tests obligatorios: `mvn test` debe pasar antes de commit.
3. Al cerrar una fase: commit `chore: finalize Fase N - descripción` + tag `v1.0.0-phaseN`.
4. CI (GitHub Actions) ejecuta build + tests en cada push/PR a `main`.

## Tests

- **Unitarios**: Mockito + MockMvc standalone (`controller/`, `service/`, `security/`).
- **Integración**: `@SpringBootTest` con H2 (`integration/`) — flujos completos login → generar → historial → logout.
- Perfil de test: `src/test/resources/application-test.properties`.

```bash
mvn test                                  # toda la suite
mvn test -Dtest=CsrServiceTest            # una clase
```

## Verificación manual de CSR

```bash
openssl req -text -noout -in archivo.csr        # inspeccionar CSR
openssl pkey -in archivo.key -text -noout       # debe pedir la contraseña (clave cifrada)
```