package com.example.demo;

import com.example.demo.dto.lista.MoverFavoritosRequest;
import com.example.demo.model.Favorito;
import com.example.demo.model.Lista;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.repository.jpa.ListaRepositoryAdapter;
import com.example.demo.service.ListaService;
import com.example.demo.service.ProductoService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Usa PostgreSQL configurado para la app, pero sólo toca el esquema tp2_tests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.default-schema=tp2_tests",
        "spring.flyway.postgresql.transactional-lock=false",
        "spring.jpa.properties.hibernate.default_schema=tp2_tests"
})
class DemoApplicationTests {
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired ListaService listaService;
    @Autowired ListaRepository listas;
    @Autowired FavoritoRepository favoritos;
    @MockitoSpyBean ListaRepositoryAdapter listaAdapter;
    // Las pruebas de persistencia no dependen de la disponibilidad de DummyJSON.
    @MockitoBean ProductoService productoService;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void limpiarSoloEsquemaDePruebas() {
        jdbc.update("DELETE FROM tp2_tests.favoritos");
        jdbc.update("DELETE FROM tp2_tests.listas");
    }

    @Test
    void crudListasFavoritosYErroresHttp() throws Exception {
        JsonNode origen = request("POST", "/api/listas", "{\"nombre\":\"Regalos\"}", 201);
        long origenId = origen.get("id").asLong();
        long destinoId = request("POST", "/api/listas", "{\"nombre\":\"Ofertas\"}", 201).get("id").asLong();
        request("GET", "/api/listas", null, 200);
        request("GET", "/api/listas/" + origenId, null, 200);
        request("POST", "/api/listas", "{\"nombre\":\"  \"}", 400);
        request("GET", "/api/listas/9223372036854775807", null, 404);
        request("GET", "/api/listas/9223372036854775807/favoritos", null, 404);
        request("DELETE", "/api/listas/9223372036854775807", null, 404);
        request("POST", "/api/listas", "{broken", 400);

        long favoritoId = request("POST", "/api/favoritos", bodyFavorito(origenId, "Inicial"), 201).get("id").asLong();
        request("GET", "/api/favoritos", null, 200);
        assertThat(request("GET", "/api/favoritos/" + favoritoId, null, 200).get("listaId").asLong()).isEqualTo(origenId);
        request("PUT", "/api/favoritos/" + favoritoId, bodyFavorito(origenId, "Actualizada"), 200);
        assertThat(request("GET", "/api/listas/" + origenId + "/favoritos", null, 200).size()).isEqualTo(1);
        request("DELETE", "/api/listas/" + origenId, null, 409);
        request("POST", "/api/favoritos", "{\"productoId\":1,\"nota\":\"Sin lista\"}", 400);
        request("POST", "/api/favoritos", bodyFavorito(Long.MAX_VALUE, "Lista inexistente"), 404);
        request("GET", "/api/favoritos/9223372036854775807", null, 404);
        request("POST", "/api/listas/" + origenId + "/mover-favoritos", "{}", 400);
        request("POST", "/api/listas/" + origenId + "/mover-favoritos", "{\"destinoId\":" + origenId + "}", 400);
        request("POST", "/api/listas/" + origenId + "/mover-favoritos", "{\"destinoId\":9223372036854775807}", 404);
        request("POST", "/api/listas/9223372036854775807/mover-favoritos", "{\"destinoId\":" + destinoId + "}", 404);
        // Los errores anteriores no movieron ni eliminaron nada.
        assertThat(request("GET", "/api/listas/" + origenId + "/favoritos", null, 200).size()).isEqualTo(1);

        JsonNode movido = request("POST", "/api/listas/" + origenId + "/mover-favoritos", "{\"destinoId\":" + destinoId + "}", 200);
        assertThat(movido.get("favoritosMovidos").asInt()).isEqualTo(1);
        request("GET", "/api/listas/" + origenId, null, 404);
        JsonNode favorito = request("GET", "/api/favoritos/" + favoritoId, null, 200);
        assertThat(favorito.get("listaId").asLong()).isEqualTo(destinoId);
        assertThat(favorito.get("nota").asText()).isEqualTo("Actualizada");
        request("DELETE", "/api/favoritos/" + favoritoId, null, 204);
        request("DELETE", "/api/listas/" + destinoId, null, 204);
        request("GET", "/api/favoritos/" + favoritoId, null, 404);
        JsonNode docs = request("GET", "/v3/api-docs", null, 200);
        assertThat(docs.get("paths").has("/api/listas/{origenId}/mover-favoritos")).isTrue();
        assertThat(docs.get("paths").has("/api/productos")).isTrue();
    }

    @Test
    void rollbackRevierteReasignacionesSiFallaEliminarOrigen() {
        Lista origen = listas.guardar(new Lista(null, "Origen rollback"));
        Lista destino = listas.guardar(new Lista(null, "Destino rollback"));
        Favorito primero = favoritos.guardar(new Favorito(null, 1L, "Uno", LocalDateTime.now(), origen.id()));
        Favorito segundo = favoritos.guardar(new Favorito(null, 2L, "Dos", LocalDateTime.now(), origen.id()));
        doThrow(new IllegalStateException("Fallo simulado después de reasignar"))
                .when(listaAdapter).eliminarPorId(origen.id());
        try {
            assertThatThrownBy(() -> listaService.moverFavoritos(origen.id(), new MoverFavoritosRequest(destino.id())))
                    .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            assertThat(listas.buscarPorId(origen.id())).isPresent();
            assertThat(favoritos.buscarPorId(primero.id()).orElseThrow().listaId()).isEqualTo(origen.id());
            assertThat(favoritos.buscarPorId(segundo.id()).orElseThrow().listaId()).isEqualTo(origen.id());
            assertThat(favoritos.buscarPorListaId(destino.id())).isEmpty();
        } finally { reset(listaAdapter); }
    }

    @Test
    void migrarFavoritoPrevioDesdeV1HastaV4SinPerderDatos() {
        String schema = "tp2_migration_" + UUID.randomUUID().toString().replace("-", "");
        Flyway inicial = Flyway.configure().configuration(java.util.Map.of("flyway.postgresql.transactional.lock", "false")).dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target("1").load();
        Flyway completa = Flyway.configure().configuration(java.util.Map.of("flyway.postgresql.transactional.lock", "false")).dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        try {
            inicial.migrate();
            jdbc.update("INSERT INTO " + schema + ".favoritos (producto_id, nota, fecha_alta) VALUES (1, 'Anterior a listas', CURRENT_TIMESTAMP)");
            completa.migrate();
            assertThat(jdbc.queryForObject("SELECT l.nombre FROM " + schema + ".favoritos f JOIN "
                    + schema + ".listas l ON l.id = f.lista_id", String.class)).isEqualTo("Sin clasificar");
            assertThat(jdbc.queryForObject("SELECT nota FROM " + schema + ".favoritos", String.class)).isEqualTo("Anterior a listas");
            assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = ? AND table_name = 'favoritos' AND column_name = 'lista_id'", String.class, schema)).isEqualTo("NO");
            assertThat(completa.migrate().migrationsExecuted).isZero();
        } finally { completa.clean(); }
    }

    private String bodyFavorito(long listaId, String nota) {
        return "{\"productoId\":1,\"nota\":\"" + nota + "\",\"listaId\":" + listaId + "}";
    }

    private JsonNode request(String method, String path, String body, int expected) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method + " " + path + " => " + response.body()).isEqualTo(expected);
        if (expected == 201) assertThat(response.headers().firstValue("Location")).isPresent();
        return response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
    }
}
