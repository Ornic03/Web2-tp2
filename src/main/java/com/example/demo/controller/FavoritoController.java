package com.example.demo.controller;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "CRUD de favoritos almacenados en PostgreSQL")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping
    @Operation(summary = "Crear un favorito")
    public ResponseEntity<FavoritoResponse> crear(@Valid @RequestBody FavoritoRequest request) {
        FavoritoResponse creado = favoritoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();

        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar favoritos")
    public List<FavoritoResponse> listar() {
        return favoritoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un favorito por id")
    public FavoritoResponse obtenerPorId(@PathVariable Long id) {
        return favoritoService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un favorito")
    public FavoritoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody FavoritoRequest request) {
        return favoritoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un favorito")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        favoritoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
