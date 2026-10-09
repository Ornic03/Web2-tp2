package com.example.demo.repository.jpa;

import com.example.demo.model.Lista;
import com.example.demo.repository.ListaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ListaRepositoryAdapter implements ListaRepository {
    private final ListaJpaRepository listas;

    public ListaRepositoryAdapter(ListaJpaRepository listas) { this.listas = listas; }

    @Override
    @Transactional
    public Lista guardar(Lista lista) {
        return mapear(listas.saveAndFlush(new ListaEntity(lista.id(), lista.nombre())));
    }

    @Override
    public List<Lista> listar() {
        return listas.findAll(Sort.by("id")).stream().map(this::mapear).toList();
    }

    @Override
    public Optional<Lista> buscarPorId(Long id) { return listas.findById(id).map(this::mapear); }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        listas.deleteById(id);
        listas.flush(); // La FK también protege contra eliminaciones con favoritos concurrentes.
    }

    private Lista mapear(ListaEntity entity) { return new Lista(entity.getId(), entity.getNombre()); }
}
