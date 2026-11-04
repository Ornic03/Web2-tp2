package com.example.demo.repository;

import com.example.demo.model.Favorito;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository {
    Favorito guardar(Favorito favorito);

    List<Favorito> listar();

    Optional<Favorito> buscarPorId(Long id);

    List<Favorito> buscarPorListaId(Long listaId);

    List<Favorito> buscarPorUsuarioId(Long usuarioId);

    void eliminarPorId(Long id);
}
