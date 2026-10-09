package com.example.demo.repository;

import com.example.demo.model.Favorito;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class FavoritoRepositoryMemoria implements FavoritoRepository {

    private final ConcurrentMap<Long, Favorito> favoritos = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    @Override
    public Favorito guardar(Favorito favorito) {
        Long id = favorito.id() == null ? secuencia.incrementAndGet() : favorito.id();
        Favorito guardado = new Favorito(id, favorito.productoId(), favorito.nota(), favorito.fechaAgregado());
        favoritos.put(id, guardado);
        return guardado;
    }

    @Override
    public List<Favorito> listar() {
        return favoritos.values()
                .stream()
                .sorted(Comparator.comparing(Favorito::id))
                .toList();
    }

    @Override
    public Optional<Favorito> buscarPorId(Long id) {
        return Optional.ofNullable(favoritos.get(id));
    }

    @Override
    public void eliminarPorId(Long id) {
        favoritos.remove(id);
    }
}
