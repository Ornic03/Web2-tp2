package com.example.demo.controller;

import com.example.demo.dto.producto.ProductoDTO;
import com.example.demo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo obtenido desde DummyJSON")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Obtiene y transforma el catálogo público de DummyJSON")
    public List<ProductoDTO> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto", description = "Busca un producto de DummyJSON por su id")
    public ProductoDTO obtenerPorId(@PathVariable Long id) {
        return productoService.obtenerPorId(id);
    }
}
