package com.example.demo.dto.lista;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListaRequest(
        @NotBlank(message = "el nombre no puede estar vacío")
        @Size(max = 100, message = "el nombre no puede superar los 100 caracteres")
        String nombre
) { }
