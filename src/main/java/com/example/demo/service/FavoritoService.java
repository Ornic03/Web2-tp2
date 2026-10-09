package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.model.Favorito;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final ProductoService productoService;
    private final ListaRepository listaRepository;

    public FavoritoService(FavoritoRepository favoritoRepository, ProductoService productoService, ListaRepository listaRepository) {
        this.favoritoRepository = favoritoRepository;
        this.productoService = productoService;
        this.listaRepository = listaRepository;
    }

    public FavoritoResponse crear(FavoritoRequest request) {
        validarLista(request.listaId());
        validarProducto(request.productoId());

        Favorito favorito = new Favorito(
                null,
                request.productoId(),
                request.nota(),
                LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
                request.listaId()
        );

        return mapear(favoritoRepository.guardar(favorito));
    }

    public List<FavoritoResponse> listar() {
        return favoritoRepository.listar()
                .stream()
                .map(this::mapear)
                .toList();
    }

    public FavoritoResponse obtenerPorId(Long id) {
        return mapear(buscar(id));
    }

    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        Favorito actual = buscar(id);
        validarLista(request.listaId());
        validarProducto(request.productoId());

        Favorito actualizado = new Favorito(
                actual.id(),
                request.productoId(),
                request.nota(),
                actual.fechaAgregado(),
                request.listaId()
        );

        return mapear(favoritoRepository.guardar(actualizado));
    }

    public void eliminar(Long id) {
        buscar(id);
        favoritoRepository.eliminarPorId(id);
    }

    private Favorito buscar(Long id) {
        return favoritoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el favorito con id " + id));
    }

    private void validarLista(Long listaId) {
        listaRepository.buscarPorId(listaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lista con id " + listaId));
    }

    private void validarProducto(Long productoId) {
        productoService.obtenerPorId(productoId);
    }

    private FavoritoResponse mapear(Favorito favorito) {
        return new FavoritoResponse(
                favorito.id(),
                favorito.productoId(),
                favorito.nota(),
                favorito.fechaAgregado(),
                favorito.listaId()
        );
    }
}
