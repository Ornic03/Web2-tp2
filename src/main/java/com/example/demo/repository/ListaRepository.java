package com.example.demo.repository;

import com.example.demo.model.Lista;
import java.util.List;
import java.util.Optional;

// Puerto: expresa operaciones del dominio sin depender de JPA.
public interface ListaRepository {
    Lista guardar(Lista lista);
    List<Lista> listar();
    Optional<Lista> buscarPorId(Long id);
    void eliminarPorId(Long id);
}
