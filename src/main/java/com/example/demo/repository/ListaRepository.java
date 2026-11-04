package com.example.demo.repository;

import com.example.demo.model.Lista;
import java.util.List;
import java.util.Optional;

public interface ListaRepository {
    Lista guardar(Lista lista);
    List<Lista> listar();
    List<Lista> listarPorUsuarioId(Long usuarioId);
    Optional<Lista> buscarPorId(Long id);
    void eliminarPorId(Long id);
}
