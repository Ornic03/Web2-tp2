package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.dto.lista.*;
import com.example.demo.exception.*;
import com.example.demo.model.Favorito;
import com.example.demo.model.Lista;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** La autorización está en el service para proteger también llamadas fuera de HTTP. */
@Service
public class ListaServiceImpl {
    private final ListaRepository listas;
    private final FavoritoRepository favoritos;
    private final UsuarioActual usuarioActual;

    public ListaServiceImpl(ListaRepository listas, FavoritoRepository favoritos, UsuarioActual usuarioActual) {
        this.listas = listas;
        this.favoritos = favoritos;
        this.usuarioActual = usuarioActual;
    }

    public ListaResponse crear(ListaRequest request) {
        return mapear(listas.guardar(new Lista(null, request.nombre().trim(), usuarioActual.id())));
    }

    public List<ListaResponse> listar() {
        List<Lista> resultado = usuarioActual.esAdmin() ? listas.listar() : listas.listarPorUsuarioId(usuarioActual.id());
        return resultado.stream().map(this::mapear).toList();
    }

    public ListaResponse obtenerPorId(Long id) { return mapear(buscarAutorizada(id)); }

    public List<FavoritoResponse> listarFavoritos(Long id) {
        buscarAutorizada(id);
        return favoritos.buscarPorListaId(id).stream()
                .map(f -> new FavoritoResponse(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId()))
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {
        buscarAutorizada(id);
        if (!favoritos.buscarPorListaId(id).isEmpty()) {
            throw new ConflictoException("No se puede eliminar una lista que tiene favoritos");
        }
        listas.eliminarPorId(id);
    }

    @Transactional
    public MoverFavoritosResponse moverFavoritos(Long origenId, MoverFavoritosRequest request) {
        buscarAutorizada(origenId);
        buscarAutorizada(request.destinoId()); // Proteger AMBOS extremos del movimiento.
        if (origenId.equals(request.destinoId())) {
            throw new SolicitudInvalidaException("La lista destino debe ser distinta de la lista origen");
        }
        List<Favorito> aMover = favoritos.buscarPorListaId(origenId);
        for (Favorito f : aMover) {
            favoritos.guardar(new Favorito(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), request.destinoId()));
        }
        listas.eliminarPorId(origenId);
        return new MoverFavoritosResponse(origenId, request.destinoId(), aMover.size());
    }

    /** 404 sólo si el id no existe; 403 si existe pero pertenece a otro usuario. */
    public Lista buscarAutorizada(Long id) {
        Lista lista = listas.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id " + id));
        if (!usuarioActual.esAdmin() && !lista.usuarioId().equals(usuarioActual.id())) {
            throw new AccesoProhibidoException("No tenés permiso sobre esta lista");
        }
        return lista;
    }

    private ListaResponse mapear(Lista lista) { return new ListaResponse(lista.id(), lista.nombre(), lista.usuarioId()); }
}
