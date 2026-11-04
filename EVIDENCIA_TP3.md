# TP3 · Evidencias para adjuntar a la entrega

> Este documento separa lo implementado de lo que necesita ejecutarse en tu equipo o cuenta.
> No se inventaron URLs públicas, ejecuciones ni capturas.

## Evidencias a generar

- [ ] **Pruebas locales:** con Docker Desktop ejecutándose, abrir terminal dentro del proyecto y ejecutar `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`). Capturar resultado final `BUILD SUCCESS` y cantidad de pruebas. Los reportes unitarios están en `target/surefire-reports` y los de integración en `target/failsafe-reports`.
- [ ] **Migraciones:** mostrar la tabla `flyway_schema_history` de PostgreSQL, con versiones V1–V6 aplicadas (puede verse también en consola al iniciar).
- [ ] **Seguridad:** capturas de `GET /api/listas` sin token → `401` y con JWT válido → `200`. Usar `requests.http` o Swagger Authorize.
- [ ] **CI:** subir el proyecto a GitHub. Revisar Actions → workflow `TP3 - Compilación y tests PostgreSQL` y adjuntar enlace a la ejecución exitosa.
- [ ] **CI detecta fallos:** cambiar temporalmente una aserción de un test para que falle, subir un commit, capturar pipeline rojo; revertir el cambio y capturar pipeline verde.
- [ ] **Despliegue:** crear base PostgreSQL remota y desplegar el Dockerfile en un proveedor (Koyeb/Render) con `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` y `PORT`.
- [ ] **Pruebas en remoto:** adjuntar URL pública y capturas de `/health` → `200`, `/api/listas` sin token → `401`, registro/login y `/api/listas` con token → `200`.
- [ ] **Logs remotos:** capturar migraciones Flyway y aplicación iniciada correctamente.

## Estado de elaboración

El código fuente, las migraciones, el workflow y el Dockerfile están preparados. Sin acceso al repositorio GitHub ni a la cuenta del proveedor no es posible crear una ejecución de CI real o generar una URL pública auténtica. La validación de Maven + Docker requiere esas herramientas y red; el entorno donde se editó el proyecto no las tenía disponibles.
