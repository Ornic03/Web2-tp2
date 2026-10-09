package com.example.demo.repository.jpa;

import com.example.demo.model.Favorito;
import com.example.demo.repository.FavoritoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class FavoritoRepositoryAdapter implements FavoritoRepository {
    private final FavoritoJpaRepository favoritos;
    private final ListaJpaRepository listas;

    public FavoritoRepositoryAdapter(FavoritoJpaRepository favoritos, ListaJpaRepository listas) {
        this.favoritos = favoritos;
        this.listas = listas;
    }

    @Override
    @Transactional
    public Favorito guardar(Favorito favorito) {
        FavoritoEntity entity = new FavoritoEntity(favorito.id(), favorito.productoId(),
                favorito.nota(), favorito.fechaAgregado(), listas.getReferenceById(favorito.listaId()));
        // El flush ejecuta SQL; no confirma la transacción exterior del Service.
        return mapear(favoritos.saveAndFlush(entity));
    }

    @Override
    public List<Favorito> listar() {
        return favoritos.findAll(Sort.by("id")).stream().map(this::mapear).toList();
    }

    @Override
    public Optional<Favorito> buscarPorId(Long id) {
        return favoritos.findById(id).map(this::mapear);
    }

    @Override
    public List<Favorito> buscarPorListaId(Long listaId) {
        return favoritos.findByListaIdOrderByIdAsc(listaId).stream().map(this::mapear).toList();
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        favoritos.deleteById(id);
        favoritos.flush();
    }

    private Favorito mapear(FavoritoEntity entity) {
        return new Favorito(entity.getId(), entity.getProductoId(), entity.getNota(),
                entity.getFechaAgregado(), entity.getLista().getId());
    }
}
