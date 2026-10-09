package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.*;
import com.example.demo.exception.*;
import com.example.demo.model.Favorito;
import com.example.demo.model.Lista;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ListaService {
    private final ListaRepository listas;
    private final FavoritoRepository favoritos;

    public ListaService(ListaRepository listas, FavoritoRepository favoritos) {
        this.listas = listas;
        this.favoritos = favoritos;
    }

    public ListaResponse crear(ListaRequest request) {
        return mapear(listas.guardar(new Lista(null, request.nombre().trim())));
    }

    public List<ListaResponse> listar() { return listas.listar().stream().map(this::mapear).toList(); }
    public ListaResponse obtenerPorId(Long id) { return mapear(buscar(id)); }

    public List<FavoritoResponse> listarFavoritos(Long id) {
        buscar(id);
        return favoritos.buscarPorListaId(id).stream()
                .map(f -> new FavoritoResponse(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId()))
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        if (!favoritos.buscarPorListaId(id).isEmpty()) {
            throw new ConflictoException("No se puede eliminar una lista que tiene favoritos");
        }
        listas.eliminarPorId(id);
    }

    // Todas las escrituras usan la misma transacción: o se confirman todas o ninguna.
    @Transactional
    public MoverFavoritosResponse moverFavoritos(Long origenId, MoverFavoritosRequest request) {
        buscar(origenId);
        buscar(request.destinoId());
        if (origenId.equals(request.destinoId())) {
            throw new SolicitudInvalidaException("La lista destino debe ser distinta de la lista origen");
        }
        List<Favorito> aMover = favoritos.buscarPorListaId(origenId);
        for (Favorito f : aMover) {
            favoritos.guardar(new Favorito(f.id(), f.productoId(), f.nota(),
                    f.fechaAgregado(), request.destinoId()));
        }
        listas.eliminarPorId(origenId);
        return new MoverFavoritosResponse(origenId, request.destinoId(), aMover.size());
    }

    private Lista buscar(Long id) {
        return listas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id " + id));
    }

    private ListaResponse mapear(Lista lista) { return new ListaResponse(lista.id(), lista.nombre()); }
}
