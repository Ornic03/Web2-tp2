# TP2 · Persistencia, migraciones y arquitectura hexagonal

Evolución del TP1 original: catálogo externo de DummyJSON (sin persistir productos), favoritos en PostgreSQL y listas para organizarlos. Se conserva el paquete `com.example.demo`, Maven, Java 25 y Spring Boot 4.1.1 del ZIP original.

Consigna: https://unvime-web-2.matiendres.workers.dev/practicos/tp2/

## 1. Probar en Windows con Docker Desktop

### Preparación

1. Descomprimí el ZIP y abrí la carpeta `web2-tp1`, donde están `pom.xml`, `mvnw.cmd` y `docker-compose.yml`.
2. Abrí Docker Desktop y esperá a que el motor esté iniciado (contenedores Linux).
3. Abrí PowerShell o CMD **en esa carpeta**. Podés escribir `powershell` en la barra de direcciones del Explorador de archivos.
4. Comprobá Java: `java -version`. Este proyecto requiere **JDK 25**, como tu TP1. El IDE también debe usar ese JDK. No necesitás instalar Maven: está incluido el wrapper.

### Levantar PostgreSQL

```powershell
docker compose up -d
docker compose ps
```

Esperá a ver el servicio `postgres` en estado `healthy`. Si todavía está iniciando, repetí `docker compose ps` unos segundos después. Para ver qué ocurre:

```powershell
docker compose logs postgres
```

El Compose crea el contenedor, la base y el volumen automáticamente. **No hace falta pulsar Run sobre la imagen en Docker Desktop ni crear tablas a mano.** Tener la imagen descargada todavía no implica que exista un contenedor corriendo. Se usa la etiqueta `postgres:18`; si descargaste otra, Compose descargará ésta automáticamente.

| Dato | Valor por defecto |
|---|---|
| Servidor | `localhost` |
| Puerto | `5432` |
| Base | `web2_tp2` |
| Usuario | `web2` |
| Contraseña local de práctica | `web2_local` |

Son valores de desarrollo local. El puerto se publica únicamente en `127.0.0.1`. La contraseña puede cambiarse por variables de entorno. El volumen está montado en `/var/lib/postgresql`, ruta adecuada para la imagen 18.

### Levantar la API

En la misma carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

Dejá esa terminal abierta. En la primera ejecución Maven descarga dependencias. Flyway aplica V1–V4 antes de que Hibernate valide las tablas. No tenés que ejecutar SQL manualmente.

Abrí:

- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Salud: http://localhost:8080/health

En macOS/Linux: `bash mvnw spring-boot:run`.

### Recorrido en Swagger

En cada endpoint: **Try it out → completar datos → Execute**.

1. `POST /api/listas` con `{"nombre":"Regalos"}`. Responde **201**. Anotá el `id` como **ORIGEN**.
2. Creá otra con `{"nombre":"Ofertas"}`. Anotá el `id` como **DESTINO**.
3. `POST /api/favoritos` con el siguiente JSON, reemplazando `2` por el ID real de ORIGEN:

```json
{
  "productoId": 1,
  "nota": "Producto para regalar",
  "listaId": 2
}
```

4. `GET /api/listas/{id}/favoritos`, con ORIGEN: **200**, muestra el favorito.
5. `DELETE /api/listas/{id}`, con ORIGEN: **409**, porque tiene favoritos.
6. `POST /api/listas/{origenId}/mover-favoritos`: usá ORIGEN en la ruta y `{"destinoId":3}` en el cuerpo, reemplazando `3` por DESTINO. Responde **200** con la cantidad movida.
7. Consultá ORIGEN: ahora responde **404**. Consultá los favoritos de DESTINO: contiene el favorito, con su mismo ID, nota y fecha.
8. Con el favorito todavía guardado, detené **sólo la API** con `Ctrl+C` y ejecutá otra vez ` .\mvnw.cmd spring-boot:run`. `GET /api/favoritos` debe conservarlo.
9. Eliminá el favorito (**204**) y después la lista DESTINO vacía (**204**).

Los IDs no necesariamente son 1, 2 y 3: V4 crea “Sin clasificar” y las secuencias pueden dejar huecos. Usá siempre los IDs devueltos.

Crear/actualizar favoritos consulta DummyJSON para comprobar `productoId`: se necesita internet. Una lista inexistente devuelve 404 antes de llamar al servicio externo.

### Ver las migraciones y los datos

Abrí otra terminal en la carpeta:

```powershell
docker compose exec postgres psql -U web2 -d web2_tp2 -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
docker compose exec postgres psql -U web2 -d web2_tp2 -c "SELECT * FROM listas ORDER BY id;"
docker compose exec postgres psql -U web2 -d web2_tp2 -c "SELECT * FROM favoritos ORDER BY id;"
```

En una base nueva, las cuatro versiones SQL deben estar aplicadas con `success = t`.

### Detener y volver a usar

- API: `Ctrl+C`.
- Base: `docker compose stop`.
- Volver a iniciar la base: `docker compose up -d`.
- `docker compose down` quita el contenedor, pero conserva el volumen y sus datos.
- **No agregues `-v` a `down` si querés conservar los datos**: esa opción elimina el volumen.

## 2. Configuración y problemas frecuentes

- **Puerto 5432 ocupado:** si ya creaste un contenedor PostgreSQL de prueba que no usás, detenelo desde Docker Desktop. Si necesitás conservarlo encendido, podés usar 5433 para este proyecto.
- **Docker no responde:** abrí Docker Desktop y esperá a que el motor esté listo.
- **Java release 25 not supported:** el IDE/terminal está usando un JDK anterior; seleccioná JDK 25 y revisá `JAVA_HOME`.
- **Connection refused:** comprobá `docker compose ps` y que URL/puerto coincidan.
- **Password authentication failed:** las variables `POSTGRES_*` inicializan una base nueva, pero no cambian la contraseña de un volumen existente. Usá las credenciales con las que se creó ese volumen.
- **Puerto 8080 ocupado:** cerrá la otra ejecución de la API o configurá `SERVER_PORT`.
- **502 al crear favorito:** no se pudo consultar DummyJSON. Comprobá tu conexión y probá `GET /api/productos/1`.

Ejemplo PowerShell para usar otro puerto de base:

```powershell
$env:POSTGRES_PORT="5433"
docker compose up -d
$env:DB_URL="jdbc:postgresql://localhost:5433/web2_tp2"
.\mvnw.cmd spring-boot:run
```

Opcionalmente copiá `.env.example` a `.env` para configurar Compose. **Spring Boot no carga ese archivo automáticamente**: si cambiás usuario/base/contraseña, configurá también `DB_URL`, `DB_USER` y `DB_PASSWORD` en la terminal o en el IDE. Los valores por defecto funcionan sin `.env`.

También sirve una instalación local de PostgreSQL con esa base y usuario, omitiendo los pasos de Docker.

## 3. Endpoints

| Método | Ruta | Éxito |
|---|---|---|
| GET | `/api/productos` | 200 |
| GET | `/api/productos/{id}` | 200 |
| POST | `/api/favoritos` | 201 + Location |
| GET | `/api/favoritos` | 200 |
| GET | `/api/favoritos/{id}` | 200 |
| PUT | `/api/favoritos/{id}` | 200 |
| DELETE | `/api/favoritos/{id}` | 204 |
| POST | `/api/listas` | 201 + Location |
| GET | `/api/listas` | 200 |
| GET | `/api/listas/{id}` | 200 |
| GET | `/api/listas/{id}/favoritos` | 200 |
| DELETE | `/api/listas/{id}` | 204 si está vacía |
| POST | `/api/listas/{origenId}/mover-favoritos` | 200 |

Mover recibe `{"destinoId": ID}` y responde `{"origenId": ID, "destinoId": ID, "favoritosMovidos": CANTIDAD}`. Si el origen está vacío, lo elimina y devuelve cantidad 0. Se rechaza mover una lista sobre sí misma (400).

La API devuelve `ProblemDetail`: 400 por validaciones/formato, 404 por recursos inexistentes, 409 por lista ocupada o conflicto de integridad, y 502 por falla de DummyJSON. `nota` conserva el límite del TP1 de 250 caracteres; `nombre` admite hasta 100; `listaId` es obligatorio y positivo.

## 4. Puertos y adaptadores: qué cambió

Un **puerto** es un contrato: `FavoritoRepository` define las operaciones que necesita el servicio. Un **adaptador** implementa ese contrato usando una tecnología. En TP1 era un mapa; ahora `FavoritoRepositoryAdapter` convierte dominio ↔ entidad JPA y delega en `FavoritoJpaRepository`. El servicio no importa Hibernate ni entidades JPA.

La migración de memoria a JPA, por sí sola, no necesita modificar el dominio, servicio, controller ni DTOs. El punto 5 agrega un requisito distinto (listas), y por eso la versión final sí incorpora `listaId` y su validación. No corresponde afirmar que todas esas clases quedaron idénticas al TP1.

| Archivo o grupo | Cambio real respecto del ZIP TP1 |
|---|---|
| `FavoritoRepositoryMemoria` | Eliminado. Era el adaptador en memoria del proyecto original. |
| `FavoritoRepository` | Conserva sus cuatro operaciones; suma `buscarPorListaId` por la funcionalidad de listas. |
| `FavoritoRepositoryAdapter`, `FavoritoEntity`, `FavoritoJpaRepository` | Nuevos: implementación JPA y mapeo a/desde el record. |
| `Favorito` | Sigue siendo un record sin anotaciones JPA. Suma `listaId`. |
| `FavoritoRequest` / `FavoritoResponse` | Suman `listaId`; el request lo valida con `@NotNull` y `@Positive`. |
| `FavoritoService` | Suma el puerto `ListaRepository`, valida existencia y propaga `listaId`. Conserva el CRUD y validación de productos del TP1. Ajusta la precisión de la fecha a microsegundos, como PostgreSQL. |
| `FavoritoController` | Métodos idénticos; sólo cambia la descripción Swagger de “memoria” a “PostgreSQL”. |
| Dominio/puerto/DTOs/servicio/controller/adaptador de listas | Nuevos para el punto 5. |
| `GlobalExceptionHandler` | Agrega 409 y casos 400 para nuevos errores y formatos inválidos. |
| `OpenApiConfig` | Actualiza título, descripción y versión a TP2. |
| `ProductoService`, `ProductoController`, `ProductoDTO`, cliente DummyJSON y sus DTOs | Exactamente iguales. El catálogo no se guarda en la base. |
| `DemoApplication`, `RestClientConfig`, `HealthController`, `PingController`, excepciones originales | Exactamente iguales. |
| `pom.xml`, `application.properties` | Dependencias de JPA/PostgreSQL/Flyway y conexión/validación de esquema. |
| `README`, `requests.http`, pruebas | Actualizados para TP2. |

`FavoritoEntity` usa `@ManyToOne(fetch = LAZY)` con `lista_id`. No hay `@OneToMany`: se consultan los favoritos mediante `findByListaIdOrderByIdAsc`. Las entidades tienen constructor vacío y campos mutables para JPA; los records del dominio siguen siendo inmutables. Se conserva `fechaAgregado` en Java/JSON y se mapea a la columna SQL `fecha_alta` pedida.

Los adaptadores delimitan transacciones cortas para cargar y convertir entidades antes de salir de persistencia, compatible con `open-in-view=false`. La FK impide borrar una lista con favoritos incluso si otra petición cambia los datos entre una validación y una escritura.

## 5. Migraciones y evolución del esquema

Flyway se configura con `spring-boot-starter-flyway` y `flyway-database-postgresql`, incluyendo la autoconfiguración necesaria en Spring Boot 4. Hibernate usa `ddl-auto=validate`: comprueba el esquema, no lo modifica.

| Migración | Efecto |
|---|---|
| `V1__create_favoritos.sql` | Tabla `favoritos` con id, producto_id, nota y fecha_alta. |
| `V2__create_listas.sql` | Tabla `listas` con id y nombre. |
| `V3__add_lista_id_a_favoritos.sql` | Agrega `lista_id` nullable, FK e índice. |
| `V4__lista_id_obligatorio.sql` | Crea “Sin clasificar” si falta, asigna favoritos sin lista y aplica NOT NULL, en ese orden. |

Convención: `V{versión}__{descripción}.sql`, con **dos guiones bajos**. Flyway ejecuta las pendientes en orden y registra su versión/checksum en `flyway_schema_history`.

**Justificación:** se agrega una migración nueva porque las anteriores ya forman parte del historial aplicado. Si se edita una anterior, cambia su checksum y Flyway rechaza la validación; además, distintas bases podrían terminar con esquemas diferentes bajo la misma versión.

La prueba `migrarFavoritoPrevioDesdeV1HastaV4SinPerderDatos` crea un esquema aislado, aplica sólo V1, inserta un favorito anterior, aplica V2–V4 y comprueba que mantiene su nota, recibe “Sin clasificar” y termina con `lista_id NOT NULL`. También verifica que volver a migrar no ejecuta versiones otra vez.

## 6. Transacciones y ACID

`ListaService.moverFavoritos` lleva `@Transactional`. Primero valida que existan ambas listas y que sean distintas; después reasigna todos los favoritos y elimina el origen. Los adaptadores participan de la misma transacción; `flush` envía SQL pero no hace un commit independiente.

**Atomicidad:** se confirma toda la operación o no se confirma nada. Sin `@Transactional`, podría guardarse la nueva lista de algunos favoritos y fallar después otra actualización o el borrado del origen, dejando el traslado incompleto. Con la transacción se deshacen todas esas escrituras si ocurre una excepción.

**Consistencia:** la FK y NOT NULL mantienen cada favorito asociado a una lista existente. **Aislamiento:** otras transacciones no ven nuestros cambios sin confirmar; se utiliza el nivel predeterminado de PostgreSQL, Read Committed. **Durabilidad:** una vez confirmado, el cambio queda en la base; el volumen de Docker conserva los datos al recrear el contenedor. Esto no implica aislamiento serializable: si una escritura concurrente impide borrar el origen, la restricción hace fallar la operación y se revierte el traslado.

La prueba de rollback provoca una excepción al borrar el origen, después de enviar las reasignaciones a la base. Comprueba que ambos favoritos siguen en el origen, que el origen existe y que el destino queda vacío.

## 7. Pruebas y evidencia

`requests.http` incluye solicitudes ejecutables con **REST Client de VS Code**, usando IDs capturados de las respuestas. También podés hacer el recorrido de Swagger del apartado 1.

Para ejecutar las pruebas automáticas, con PostgreSQL iniciado:

```powershell
.\mvnw.cmd test
```

No hace falta tener la API iniciada. La suite levanta su propia API en un puerto libre. Usa el esquema **`tp2_tests`**, que limpia entre casos; no borra datos del esquema normal `public`. No ejecutes suites simultáneas sobre ese mismo esquema de pruebas.

- CRUD HTTP de listas/favoritos, códigos 201/200/204, `Location`, errores 400/404/409, traslado correcto y documentación OpenAPI.
- Rollback después de haber ejecutado las reasignaciones.
- Evolución desde V1 con datos anteriores hasta V4 y segunda ejecución sin migraciones pendientes.

DummyJSON se simula sólo en la suite para que estas pruebas de persistencia no dependan de internet. La aplicación normal conserva el cliente real del TP1. Consultá `EVIDENCIA.md` para el resultado de las verificaciones realizadas y sus límites.