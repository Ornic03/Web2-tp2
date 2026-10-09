package com.example.demo.dto.favorito;

import java.time.LocalDateTime;

public record FavoritoResponse(
        Long id,
        Long productoId,
        String nota,
        LocalDateTime fechaAgregado,
        Long listaId
) {
}
