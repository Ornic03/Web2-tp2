package com.example.demo.dto.favorito;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FavoritoRequest(
        @NotNull(message = "el productoId es obligatorio")
        @Positive(message = "el productoId debe ser mayor que 0")
        Long productoId,

        @NotBlank(message = "la nota no puede estar vacía")
        @Size(max = 250, message = "la nota no puede superar los 250 caracteres")
        String nota,

        @NotNull(message = "el listaId es obligatorio")
        @Positive(message = "el listaId debe ser mayor que 0")
        Long listaId
) {
}
