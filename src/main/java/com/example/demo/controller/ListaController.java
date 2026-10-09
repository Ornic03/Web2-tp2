package com.example.demo.controller;

import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.*;
import com.example.demo.service.ListaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/listas")
@Tag(name = "Listas", description = "Organización de favoritos en PostgreSQL")
public class ListaController {
    private final ListaService service;
    public ListaController(ListaService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Crear una lista", description = "Crea una lista vacía y devuelve 201 con su ubicación.")
    public ResponseEntity<ListaResponse> crear(@Valid @RequestBody ListaRequest request) {
        ListaResponse creada = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar listas", description = "Devuelve todas las listas ordenadas por id.")
    public List<ListaResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una lista", description = "Devuelve 404 si la lista no existe.")
    public ListaResponse obtener(@PathVariable Long id) { return service.obtenerPorId(id); }

    @GetMapping("/{id}/favoritos")
    @Operation(summary = "Consultar favoritos de una lista", description = "Lista sus favoritos; devuelve 404 si no existe la lista.")
    public List<FavoritoResponse> favoritos(@PathVariable Long id) { return service.listarFavoritos(id); }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una lista vacía", description = "Devuelve 204, 404 si no existe o 409 si todavía tiene favoritos.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{origenId}/mover-favoritos")
    @Operation(summary = "Mover favoritos y eliminar lista origen",
            description = "Reasigna todos los favoritos y elimina el origen en una sola transacción. "
                    + "Devuelve 200, 404 si falta una lista o 400 si origen y destino coinciden.")
    public MoverFavoritosResponse mover(@PathVariable Long origenId,
                                        @Valid @RequestBody MoverFavoritosRequest request) {
        return service.moverFavoritos(origenId, request);
    }
}
