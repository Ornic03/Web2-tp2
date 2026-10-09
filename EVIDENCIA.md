# Verificaciones realizadas

## Entorno y alcance

Se compiló y empaquetó con Java 25.0.4.1 y Spring Boot 4.1.1. No había Docker disponible en el entorno de verificación, por lo que se ejecutó contra **PostgreSQL 18.3 mediante PGlite 0.5.8 (WebAssembly)**, conectado por TCP con el driver PostgreSQL real, JPA/Hibernate y Flyway. No se usó H2 ni un repositorio en memoria para persistir favoritos.

Esto verifica el SQL, las migraciones y la integración, pero **no equivale a haber ejecutado el contenedor oficial en Docker Desktop**. El README incluye los pasos para completar esa comprobación en tu equipo con `postgres:18`.

En las pruebas se usaron bloqueos de sesión de Flyway para compatibilidad con PGlite; la configuración normal de la aplicación mantiene el comportamiento predeterminado para PostgreSQL. La base de pruebas usa un esquema separado.

## Resultado de la suite

El reporte de Maven se incluye en `evidencias/pruebas-maven.txt`:

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

1. **CRUD y errores HTTP:** creación/listado/consulta/eliminación de listas; CRUD de favoritos; `Location` en creaciones; errores 400, 404 y 409; movimiento exitoso con eliminación del origen y conservación de la nota; rutas documentadas en OpenAPI.
2. **Rollback:** se crearon dos favoritos, se enviaron sus reasignaciones a la base y se provocó una excepción al eliminar el origen. Ambos favoritos conservaron la lista original, el origen siguió existiendo y el destino quedó vacío.
3. **Evolución del esquema:** se aplicó V1 en un esquema nuevo, se insertó un favorito antiguo y se aplicaron V2–V4. Se conservó su nota, quedó asociado a “Sin clasificar” y la columna terminó como NOT NULL. Una nueva ejecución no aplicó migraciones otra vez.

DummyJSON se reemplaza por un mock de servicio únicamente en esta suite.

## Comprobación adicional con el JAR ejecutable

Se inició la aplicación empaquetada y se verificó que Swagger respondiera HTTP 200. Se creó una lista y un favorito mediante HTTP; después se detuvieron **la API y la base**, se iniciaron nuevamente sobre los mismos datos y se consultó el favorito.

Resultado: HTTP 200 y el mismo contenido antes y después:

```json
{
  "id": 2,
  "productoId": 1,
  "nota": "Conservar después del reinicio",
  "fechaAgregado": "2026-10-01T20:41:47.210178",
  "listaId": 4
}
```

Los IDs son los de esa ejecución de verificación; no se incluyen datos precargados de esta prueba en el proyecto entregado. Para esta comprobación, el cliente externo recibió desde un servidor local una respuesta JSON descargada de DummyJSON. La aplicación entregada conserva la URL real de DummyJSON.

Durante esta comprobación se ajustó la fecha de creación a microsegundos para que la respuesta inicial y la posterior lectura de PostgreSQL tengan la misma precisión. El empaquetado y el reinicio se verificaron después de ese ajuste.

## Comprobación en Docker Desktop

Ejecutá el recorrido del README y `mvnw.cmd test` con el contenedor iniciado. `requests.http` contiene también los casos de éxito y error para repetirlos manualmente y tomar capturas si te las solicitan.
