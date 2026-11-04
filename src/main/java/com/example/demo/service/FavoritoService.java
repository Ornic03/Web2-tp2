package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoRequest;
import com.example.demo.dto.favorito.FavoritoResponse;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.model.Favorito;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FavoritoService {
    private final FavoritoRepository favoritos;
    private final ProductoService productos;
    private final ListaServiceImpl listas;
    private final UsuarioActual usuarioActual;

    public FavoritoService(FavoritoRepository favoritos, ProductoService productos,
                           ListaServiceImpl listas, UsuarioActual usuarioActual) {
        this.favoritos = favoritos;
        this.productos = productos;
        this.listas = listas;
        this.usuarioActual = usuarioActual;
    }

    public FavoritoResponse crear(FavoritoRequest request) {
        listas.buscarAutorizada(request.listaId());
        productos.obtenerPorId(request.productoId());
        return mapear(favoritos.guardar(new Favorito(null, request.productoId(), request.nota(),
                LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), request.listaId())));
    }

    public List<FavoritoResponse> listar() {
        // Filtrar EN la base de datos: ni siquiera se cargan registros ajenos.
        List<Favorito> encontrados = usuarioActual.esAdmin()
                ? favoritos.listar() : favoritos.buscarPorUsuarioId(usuarioActual.id());
        return encontrados.stream().map(this::mapear).toList();
    }

    public FavoritoResponse obtenerPorId(Long id) { return mapear(buscarAutorizado(id)); }

    @Transactional
    public FavoritoResponse actualizar(Long id, FavoritoRequest request) {
        Favorito actual = buscarAutorizado(id);
        listas.buscarAutorizada(request.listaId());
        productos.obtenerPorId(request.productoId());
        return mapear(favoritos.guardar(new Favorito(actual.id(), request.productoId(),
                request.nota(), actual.fechaAgregado(), request.listaId())));
    }

    @Transactional
    public void eliminar(Long id) {
        buscarAutorizado(id);
        favoritos.eliminarPorId(id);
    }

    private Favorito buscarAutorizado(Long id) {
        Favorito f = favoritos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el favorito con id " + id));
        listas.buscarAutorizada(f.listaId());
        return f;
    }

    private FavoritoResponse mapear(Favorito f) {
        return new FavoritoResponse(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), f.listaId());
    }
}
