package com.example.demo;

import com.example.demo.dto.producto.ProductoDTO;
import com.example.demo.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSeguridadIT {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("web2_tp3_tests")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void conexionPostgresReal(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "clave-de-test-de-integracion-32-caracteres-2026");
    }

    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired ObjectMapper json;
    @MockitoBean ProductoService productos;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void limpiarDatos() {
        jdbc.update("DELETE FROM favoritos");
        jdbc.update("DELETE FROM listas");
        jdbc.update("DELETE FROM usuarios");
        when(productos.obtenerPorId(anyLong())).thenReturn(
                new ProductoDTO(1L, "Producto test", "", "", "", 1.0, 1, 5.0, ""));
    }

    @Test
    void migracionesV1AV6ArrancanContraPostgresLimpio() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(count).isEqualTo(6);
        Integer columnas = jdbc.queryForObject("SELECT count(*) FROM information_schema.columns "
                + "WHERE table_name = 'listas' AND column_name = 'usuario_id' AND is_nullable = 'NO'", Integer.class);
        assertThat(columnas).isEqualTo(1);
    }

    @Test
    void registroLoginValidoInvalidoYDuplicado() throws Exception {
        JsonNode registro = llamar("POST", "/api/auth/registro", "{\"email\":\"Ana@Ejemplo.com\",\"password\":\"contraseña1234\"}", null, 201);
        assertThat(registro.get("email").asText()).isEqualTo("ana@ejemplo.com");
        assertThat(registro.get("rol").asText()).isEqualTo("USER");
        assertThat(registro.has("passwordHash")).isFalse();
        llamar("POST", "/api/auth/registro", "{\"email\":\"ana@ejemplo.com\",\"password\":\"contraseña1234\"}", null, 409);
        assertThat(login("ana@ejemplo.com", "contraseña1234")).isNotBlank();
        llamar("POST", "/api/auth/login", "{\"email\":\"ana@ejemplo.com\",\"password\":\"incorrecta\"}", null, 401);
        llamar("POST", "/api/auth/login", "{\"email\":\"nadie@ejemplo.com\",\"password\":\"contraseña1234\"}", null, 401);
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuarios WHERE email = 'ana@ejemplo.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain("contraseña1234");
    }

    @Test
    void propietario403ParaOtrosAdmin200YListadoRestringido() throws Exception {
        long ana = registrar("ana@ejemplo.com");
        long bea = registrar("bea@ejemplo.com");
        String tokenAna = login("ana@ejemplo.com", "clave12345");
        String tokenBea = login("bea@ejemplo.com", "clave12345");
        long listaAna = llamar("POST", "/api/listas", "{\"nombre\":\"Lista Ana\",\"usuarioId\":" + bea + "}", tokenAna, 201).get("id").asLong();
        long listaBea = llamar("POST", "/api/listas", "{\"nombre\":\"Lista Bea\"}", tokenBea, 201).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM listas WHERE id = ?", Long.class, listaAna)).isEqualTo(ana);

        llamar("GET", "/api/listas/" + listaAna, null, tokenAna, 200);
        llamar("GET", "/api/listas/" + listaBea, null, tokenAna, 403);
        llamar("GET", "/api/listas/" + listaAna, null, tokenBea, 403);
        llamar("GET", "/api/listas/" + listaAna + "/favoritos", null, tokenBea, 403);
        llamar("DELETE", "/api/listas/" + listaAna, null, tokenBea, 403);
        llamar("POST", "/api/listas/" + listaAna + "/mover-favoritos", "{\"destinoId\":" + listaBea + "}", tokenAna, 403);
        llamar("GET", "/api/listas/99999999", null, tokenAna, 404);
        JsonNode propias = llamar("GET", "/api/listas", null, tokenAna, 200);
        assertThat(propias.size()).isEqualTo(1);
        assertThat(propias.get(0).get("id").asLong()).isEqualTo(listaAna);

        // No hay endpoint público para asignar ADMIN: sólo el operador de DB puede promoverlo.
        jdbc.update("UPDATE usuarios SET rol = 'ADMIN' WHERE id = ?", bea);
        String tokenAdmin = login("bea@ejemplo.com", "clave12345");
        llamar("GET", "/api/listas/" + listaAna, null, tokenAdmin, 200);
        assertThat(llamar("GET", "/api/listas", null, tokenAdmin, 200).size()).isEqualTo(2);
    }

    @Test
    void peticionesSinTokenConTokenFalsoYProteccionDeFavoritos() throws Exception {
        llamar("GET", "/api/listas", null, null, 401);
        llamar("GET", "/api/listas", null, "token-falso", 401);
        llamar("GET", "/api/favoritos", null, null, 401);
        llamar("GET", "/health", null, null, 200);
        llamar("GET", "/api/productos", null, null, 200);
        JsonNode docs = llamar("GET", "/v3/api-docs", null, null, 200);
        assertThat(docs.get("paths").has("/api/auth/login")).isTrue();
        assertThat(docs.get("components").get("securitySchemes").has("bearerAuth")).isTrue();
        registrar("a@ejemplo.com");
        registrar("b@ejemplo.com");
        String tokenA = login("a@ejemplo.com", "clave12345");
        String tokenB = login("b@ejemplo.com", "clave12345");
        long listaA = llamar("POST", "/api/listas", "{\"nombre\":\"A\"}", tokenA, 201).get("id").asLong();
        long listaB = llamar("POST", "/api/listas", "{\"nombre\":\"B\"}", tokenB, 201).get("id").asLong();
        String favoritoBody = "{\"productoId\":1,\"nota\":\"A\",\"listaId\":" + listaA + "}";
        long id = llamar("POST", "/api/favoritos", favoritoBody, tokenA, 201).get("id").asLong();
        llamar("GET", "/api/favoritos/" + id, null, tokenB, 403);
        llamar("PUT", "/api/favoritos/" + id, favoritoBody, tokenB, 403);
        llamar("DELETE", "/api/favoritos/" + id, null, tokenB, 403);
        llamar("POST", "/api/favoritos", favoritoBody, tokenB, 403);
        llamar("PUT", "/api/favoritos/" + id,
                "{\"productoId\":1,\"nota\":\"Cambio\",\"listaId\":" + listaB + "}", tokenA, 403);
        assertThat(llamar("GET", "/api/favoritos", null, tokenB, 200).size()).isZero();
        assertThat(llamar("GET", "/api/favoritos", null, tokenA, 200).size()).isEqualTo(1);
    }

    @Test
    void moverFavoritosPropiosPreservaDatosYEvitaEliminarListaLlena() throws Exception {
        registrar("movimiento@ejemplo.com");
        String token = login("movimiento@ejemplo.com", "clave12345");
        long origen = llamar("POST", "/api/listas", "{\"nombre\":\"Origen\"}", token, 201).get("id").asLong();
        long destino = llamar("POST", "/api/listas", "{\"nombre\":\"Destino\"}", token, 201).get("id").asLong();
        long favId = llamar("POST", "/api/favoritos", "{\"productoId\":1,\"nota\":\"Regalo\",\"listaId\":" + origen + "}", token, 201).get("id").asLong();
        llamar("DELETE", "/api/listas/" + origen, null, token, 409);
        JsonNode movido = llamar("POST", "/api/listas/" + origen + "/mover-favoritos",
                "{\"destinoId\":" + destino + "}", token, 200);
        assertThat(movido.get("favoritosMovidos").asInt()).isEqualTo(1);
        llamar("GET", "/api/listas/" + origen, null, token, 404);
        assertThat(llamar("GET", "/api/favoritos/" + favId, null, token, 200)
                .get("listaId").asLong()).isEqualTo(destino);
        llamar("DELETE", "/api/favoritos/" + favId, null, token, 204);
        llamar("DELETE", "/api/listas/" + destino, null, token, 204);
    }

    @Test
    void historicosDelTp2SeConservanAlMigrarDesdeV1() {
        String schema = "migracion_tp3_" + UUID.randomUUID().toString().replace("-", "");
        Flyway antes = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").target("1").load();
        Flyway despues = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .locations("classpath:db/migration").cleanDisabled(false).load();
        try {
            antes.migrate();
            jdbc.update("INSERT INTO " + schema + ".favoritos (producto_id, nota, fecha_alta) "
                    + "VALUES (1, 'Guardado antes del TP2', CURRENT_TIMESTAMP)");
            despues.migrate();
            String nota = jdbc.queryForObject("SELECT nota FROM " + schema + ".favoritos", String.class);
            assertThat(nota).isEqualTo("Guardado antes del TP2");
            String duenio = jdbc.queryForObject("SELECT u.email FROM " + schema + ".listas l "
                    + "JOIN " + schema + ".usuarios u ON l.usuario_id=u.id "
                    + "WHERE l.id=(SELECT f.lista_id FROM " + schema + ".favoritos f LIMIT 1)", String.class);
            assertThat(duenio).isEqualTo("legacy@local.invalid");
        } finally {
            despues.clean();
        }
    }

    private long registrar(String email) throws Exception {
        return llamar("POST", "/api/auth/registro",
                "{\"email\":\"" + email + "\",\"password\":\"clave12345\"}", null, 201).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        JsonNode response = llamar("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", null, 200);
        return response.get("token").asText();
    }

    private JsonNode llamar(String metodo, String path, String body, String jwt, int esperado) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (jwt != null) request.header("Authorization", "Bearer " + jwt);
        HttpResponse<String> response = http.send(request.method(metodo,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(metodo + " " + path + ": " + response.body()).isEqualTo(esperado);
        return response.body().isBlank() ? json.nullNode() : json.readTree(response.body());
    }
}
