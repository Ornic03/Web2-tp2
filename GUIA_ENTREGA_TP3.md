# Guía paso a paso para entregar el TP3 de Web II

> Esta guía es para **Windows 10 y Visual Studio Code**. El proyecto implementado está listo para intentar compilar y probar, pero **no se adjuntan pruebas ejecutadas ni URL pública**: esas evidencias se obtienen siguiendo estos pasos en tu PC, GitHub y Render. Si algún test no pasa, no lo entregues como exitoso: corregí el fallo y repetí `verify`.

## 1. Preparativos

Instalá **JDK 21**, **Docker Desktop** (con WSL2 si corresponde), **Git** y **Visual Studio Code**. Iniciá Docker Desktop y esperá a que esté activo. En PowerShell, comprobá:

```powershell
java -version
docker --version
docker info
git --version
```

## 2. Descomprimir y ejecutar

Extraé `Web2-TP3-Entrega-Corregida.zip`, abrí la carpeta `Web2-TP3-Entrega` en VS Code y abrí **Terminal → Nuevo terminal**. Debés ver `pom.xml` en esa carpeta.

```powershell
docker compose up -d
docker compose ps
.\mvnw.cmd spring-boot:run
```

Dejá esa terminal abierta. En el navegador:

- `http://localhost:8080/health` → JSON con `status: ok`.
- `http://localhost:8080/swagger-ui/index.html` → panel Swagger.

La primera ejecución Maven puede descargar las dependencias. Si el puerto 5432 está ocupado, detené el servicio que lo usa o modificá `POSTGRES_PORT` y `DB_URL` de forma coherente. **No borres volúmenes de PostgreSQL si contienen datos que querés conservar**.

## 3. Probar manualmente la seguridad

En Swagger usá `POST /api/auth/registro` con:

```json
{"email":"alumno@ejemplo.com","password":"ClaveSegura123"}
```

Debe dar `201`; repetido, `409`. Luego `POST /api/auth/login` con el mismo JSON → `200` con un campo `token`. En **Authorize** de Swagger ingresá el JWT; Swagger agrega el prefijo `Bearer` automáticamente.

Usá `POST /api/listas` con `{"nombre":"Mi lista"}` → `201`. `GET /api/listas` con token → `200`. Cerrá la autorización y repetí `GET /api/listas` → `401`.

Para probar `403`, creá un segundo usuario, creá una lista con el primero e intentá abrir `GET /api/listas/{id}` con el token del segundo. Como administrador autorizado, podés asignar el rol ADMIN en la base **solo para pruebas**, volver a iniciar sesión y comprobar `200` en listas ajenas.

## 4. Pruebas automáticas y migraciones (OBLIGATORIO)

En una terminal nueva, con Docker Desktop funcionando:

```powershell
.\mvnw.cmd clean verify
```

Resultado necesario: `BUILD SUCCESS`. `mvn test` **no** es suficiente, pues corre únicamente `*Test` y omite `*IT` de Testcontainers. Los resultados se guardan en `target/surefire-reports` (unitarias) y `target/failsafe-reports` (integración).

Para mostrar V1–V6 aplicadas en la base local, con la aplicación iniciada:

```powershell
docker compose exec postgres psql -U web2 -d web2_tp2 -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Capturá la consola con `BUILD SUCCESS` y la tabla de Flyway.

## 5. Subir al **mismo repositorio** del TP2

La consigna pide continuar el repositorio TP2, no necesariamente crear otro. Si ya lo tenés clonado, **guardá una copia**, copiá dentro el contenido completo de `Web2-TP3-Entrega`, reemplazá los archivos de la versión TP2 y **preservá la carpeta `.git` existente**.

Si no tenés el repositorio local, clonalo desde GitHub y luego copiá dentro el contenido del TP3, sin eliminar `.git`:

```powershell
git clone https://github.com/TU_USUARIO/TU_REPO_TP2.git
cd TU_REPO_TP2
# Copiar aquí el contenido de Web2-TP3-Entrega mediante el Explorador de archivos.
git add -A
git commit -m "TP3: seguridad JWT, autorizacion y pruebas CI"
git push origin main
```

Si la rama se llama `master`, usá `master` en lugar de `main`. Si no tenés repo de TP2, creá un repo **vacío** en GitHub y usá `git init`, `git branch -M main`, `git remote add origin URL` y `git push -u origin main`.

Revisá que aparezcan `.github/workflows/ci.yml`, `pom.xml`, `src/` y `README.md`. No subas `.env`, contraseñas ni secretos de producción.

## 6. GitHub Actions

En GitHub → tu repo → **Actions** → workflow **TP3 - Compilación y tests PostgreSQL** → última ejecución. Debe estar **verde** y haber corrido `bash mvnw -B -ntp clean verify`. Guardá una captura y copiá el **enlace específico de la ejecución**.

La consigna también pide demostrar que CI detecta un fallo: cambiá temporalmente en `src/test/java/com/example/demo/security/JwtServiceTest.java` el `12L` esperado en `new UsuarioAutenticado(12L, Rol.ADMIN)` por `99L` (solo el esperado). Hacé commit y push; obtené captura del workflow **rojo**. Restaurá `12L`, hacé otro commit y push y capturá el workflow **verde**. **Entregá el último commit correcto**.

## 7. Desplegar en Render (opción gratuita de evaluación)

1. En https://dashboard.render.com, registrate/iniciá sesión. Elegí **New → Postgres**. Definí un nombre y elegí plan **Free** si está disponible. Guardá por privado `hostname`, `database`, `username`, `password` y `port`. La base gratuita de Render **vence a los 30 días**.
2. En Render, **New → Web Service** → conectá GitHub → seleccioná el **mismo repositorio** y rama del TP3. Elegí **Docker** como entorno/runtime (detectará el `Dockerfile` de la raíz) y plan **Free** si está disponible. Usá la **misma región** que PostgreSQL.
3. En **Environment**, agregá estas variables, usando los datos reales de tu base:
   - `DB_URL` → `jdbc:postgresql://HOST_INTERNO:5432/NOMBRE_DB?sslmode=require`
   - `DB_USER` → nombre de usuario del PostgreSQL remoto
   - `DB_PASSWORD` → contraseña del PostgreSQL remoto
   - `JWT_SECRET` → cadena aleatoria distinta de la local, al menos 32 bytes
   - `PORT` → `8080`
4. Generá el secreto en PowerShell (NO lo subas a GitHub):

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

5. **Deploy**; en **Logs** verificá que Flyway corrió y la app arrancó sin errores; guardá captura. Render te da una URL `https://NOMBRE.onrender.com` (el valor exacto lo proporciona Render).

> La URL de Render PostgreSQL con prefijo `postgres://` **no** se pega directamente en `DB_URL`: Spring JDBC requiere `jdbc:postgresql://HOST:PUERTO/BASE`. Si el servicio y la DB están en el mismo workspace y región, preferí el host **interno**.

## 8. Capturas de la URL pública (OBLIGATORIO)

Reemplazá la URL de ejemplo por la real:

```powershell
$base = 'https://TU_SERVICIO.onrender.com'
curl.exe -i "$base/health"
curl.exe -i "$base/api/listas"
```

Se espera `200` con `/health` y `401` sin token. Registrá un usuario remoto (es una base nueva), iniciá sesión y usá el JWT **sin mostrarlo en capturas**:

```powershell
$body = '{"email":"demo@ejemplo.com","password":"ClaveSegura123"}'
Invoke-RestMethod -Method Post -Uri "$base/api/auth/registro" -ContentType 'application/json' -Body $body
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body $body
curl.exe -i -H "Authorization: Bearer $($login.token)" "$base/api/listas"
```

Se espera `200` para `/api/listas` autenticado, aunque devuelva `[]` si la lista está vacía. Guardá captura de **las 3 pruebas** (health 200, sin token 401, con token 200), ocultando credenciales y tokens.

## 9. Documentar y entregar

Editá `EVIDENCIA_TP3.md` e incluí los enlaces **reales** de tu repositorio, ejecución exitosa de Actions y URL pública. Creá una carpeta `evidencias/tp3/` con capturas legibles: prueba local `BUILD SUCCESS`, historial Flyway, seguridad, Actions rojo y verde, despliegue, logs y endpoints públicos. Actualizá el `README` con los pasos de despliegue realmente seguidos, si cambiaron respecto a los documentados. Commit final + push; revisá que Actions quede verde.

Formulario enlazado desde la materia: https://forms.gle/PsZ5xwswDZwiCDF8A . Completá los campos que realmente solicite y compartí los vínculos a GitHub, al workflow exitoso y al deploy. Guardá comprobante de envío. La consigna oficial está en https://unvime-web-2.matiendres.workers.dev/practicos/tp3/.

## Lista de verificación

- [ ] Java, Docker y Git funcionando.
- [ ] API local y Swagger funcionando.
- [ ] Registro, login y JWT comprobados.
- [ ] `clean verify`: `BUILD SUCCESS`.
- [ ] Migraciones V1–V6 aplicadas.
- [ ] Código subido al mismo repo TP2.
- [ ] Workflow de CI verde con URL real.
- [ ] Prueba de workflow rojo y posterior verde.
- [ ] Despliegue remoto con DB y JWT configurados.
- [ ] `/health` 200, `/api/listas` 401 sin token y 200 con token.
- [ ] `README` y `EVIDENCIA_TP3.md` completos con enlaces reales.
- [ ] Evidencias capturadas y entrega registrada en el formulario.
