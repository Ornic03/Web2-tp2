package com.example.demo.dto.lista;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoverFavoritosRequest(
        @NotNull(message = "el destinoId es obligatorio")
        @Positive(message = "el destinoId debe ser mayor que 0")
        Long destinoId
) { }
