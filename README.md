# TP3 · Seguridad, pruebas e integración continua

Este proyecto **continúa el TP2 dentro del mismo paquete y repositorio** (`com.example.demo`): Java **21**, Spring Boot **4.1.1**, PostgreSQL, JPA, Flyway V1–V6, JWT, BCrypt, Swagger, Testcontainers y GitHub Actions.

**Consigna TP3:** https://unvime-web-2.matiendres.workers.dev/practicos/tp3/

## Inicio rápido (Windows)

1. Instalar **JDK 21** y **Docker Desktop** (iniciar Docker antes de trabajar).
2. Abrir una terminal PowerShell en la carpeta que contiene `pom.xml`.
3. Levantar la base de la aplicación: `docker compose up -d`.
4. Ejecutar: `.\mvnw.cmd spring-boot:run`.
5. Abrir http://localhost:8080/swagger-ui/index.html y luego `POST /api/auth/registro` con `{"email":"alumno@ejemplo.com","password":"ClaveSegura123"}`.
6. Ejecutar `POST /api/auth/login` con esas mismas credenciales, copiar el campo `token`.
7. En Swagger usar **Authorize**, pegar el token y probar `POST /api/listas` con `{"nombre":"Regalos"}` y `GET /api/listas`.

No se envía `usuarioId` en el body: se obtiene exclusivamente del JWT. Catálogo público `/api/productos`, autenticación pública `/api/auth/**`, salud pública `/health`. Las rutas de listas y favoritos requieren JWT.

## Ejecutar pruebas

Con **Docker Desktop funcionando** y Java 21, en la carpeta del proyecto:

```powershell
.\mvnw.cmd clean verify
```

En Linux/macOS: `bash mvnw clean verify`. Los tests de integración (`ApiSeguridadIT`) con Testcontainers levantan su **propio PostgreSQL 16**, diferente del `docker-compose`, y Flyway aplica las 6 migraciones desde cero. No requieren crear manualmente otra base ni modificar `application.properties`. `mvn test` ejecuta sólo las pruebas unitarias (`*Test`, Surefire); `mvn verify` ejecuta **unitarias e integración** (`*IT`, Failsafe), y requiere Docker Desktop. Informes Maven: `target/surefire-reports/` y `target/failsafe-reports/`.

Se comprueba registro, hash BCrypt, login válido (200), inválido (401), listas propias (200), ajenas (403), petición sin token (401), rol ADMIN (200), favoritos, aislamiento, OpenAPI y esquema migrado.

## Autorización: ¿dónde vive y por qué?

La regla está **en la capa de servicios**: `ListaServiceImpl.buscarAutorizada` valida si una lista existe (404) y luego si su dueño coincide con el usuario del JWT (403), excepto ADMIN. También se valida el **origen Y el destino** al mover favoritos; `FavoritoService` verifica la lista asociada antes de consultar, crear, modificar o eliminar favoritos. `GET /api/listas` filtra por usuario; el admin ve todas. Al crear, el `usuarioId` se toma de `UsuarioActual`, **nunca del cliente**. La implementación `UsuarioActualSecurity` lee la identidad colocada por `JwtAuthenticationFilter` en el contexto de Spring Security.

Se hizo en servicios y no en controllers para que el control se mantenga cuando esa operación sea invocada por un test, otro controlador o código interno; se evita duplicación y fallos de autorización. SecurityFilterChain protege los endpoints, mientras los services deciden la autorización *por recurso*.

## Registro y roles

- `POST /api/auth/registro`: `{"email":"alumno@ejemplo.com","password":"ClaveSegura123"}` → **201**, rol `USER`; email duplicado → **409**.
- `POST /api/auth/login`: mismo JSON → **200** con `{ "token": "...", "tipo": "Bearer", "expiraEnSegundos": 3600 }`; credenciales equivocadas → **401**.
- Peticiones privadas: header `Authorization: Bearer TOKEN`. Sin JWT/expirado/inválido → **401**.
- **ADMIN:** el registro no permite reclamar privilegios. Para pruebas, un administrador de la base puede hacer `UPDATE usuarios SET rol='ADMIN' WHERE email='alumno@ejemplo.com';` y volver a iniciar sesión para emitir un token nuevo con rol actualizado. No hay endpoint público de elevación de privilegios.

La migración `V6` conserva las listas antiguas: las asigna a la cuenta deshabilitada `legacy@local.invalid`, con hash no válido. Un ADMIN puede consultarlas. Se preservan sin reescribir las migraciones V1–V4 originales.

## Entorno / credenciales

| Variable | Local | En hosting |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/web2_tp2` | JDBC remoto de PostgreSQL (`jdbc:postgresql://host:5432/db`) |
| `DB_USER` | `web2` | usuario del proveedor |
| `DB_PASSWORD` | `web2_local` (sólo desarrollo) | contraseña real, **en variables de entorno** |
| `JWT_SECRET` | clave de desarrollo incluida como default | **clave aleatoria única de al menos 32 bytes** |
| `JWT_EXPIRATION_MINUTES` | `60` | p. ej. `60` |
| `PORT` | `8080` | puerto requerido por proveedor |

En producción **no** utilizar la clave JWT ni las credenciales por defecto. Generar secreto en PowerShell:

```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

## Integración continua (CI)

`.github/workflows/ci.yml` se ejecuta en cada **push** y **pull_request**: checkout, JDK 21, Docker disponible, `bash mvnw -B -ntp clean verify`. El contenedor PostgreSQL se inicia desde las pruebas `*IT` y Flyway hace las migraciones. En el repositorio de GitHub: **Actions → TP3 - Compilación y tests PostgreSQL**. Si un test falla, el job termina rojo; tras corregirlo, un nuevo push debe quedar verde. Sólo se puede generar un enlace de corrida cuando el repositorio se haya subido a una cuenta GitHub.

## Despliegue (Koyeb, Render o servidor Docker)

1. Crear una instancia **PostgreSQL remota** (en el proveedor o aparte) y obtener hostname, puerto, nombre de base, usuario y contraseña.
2. Subir el proyecto a GitHub. Crear un servicio **Web desde repositorio con Dockerfile**. Se incluye `Dockerfile` multi-stage para compilar con Java 21 y lanzar el JAR.
3. Configurar `DB_URL=jdbc:postgresql://HOST:5432/DB`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y `PORT=8080` como secretos/variables en el servicio. Si tu base remota exige TLS, añadir `?sslmode=require` al JDBC URL (o la política que indique el proveedor).
4. Desplegar. Comprobar en los logs que Flyway aplicó V1–V6 y que Spring Boot escuchó en el puerto correcto.
5. Probar `https://TU-URL/health`, `/api/listas` sin token (401), registro/login y después `/api/listas` con token (200). Capturas y URL van en `EVIDENCIA_TP3.md`.

El despliegue **requiere** tu cuenta y base remota; no se puede afirmar que esté publicado hasta completar esos pasos. Consultar `EVIDENCIA_TP3.md` para el checklist de capturas y comprobaciones. El catálogo `/api/productos` requiere conexión a DummyJSON.

---

Para consultar instrucciones y contexto del TP2 original, ver `README_TP2_HISTORICO.md`.
