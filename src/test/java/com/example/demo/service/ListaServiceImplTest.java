package com.example.demo.service;

import com.example.demo.dto.lista.ListaRequest;
import com.example.demo.dto.lista.MoverFavoritosRequest;
import com.example.demo.exception.AccesoProhibidoException;
import com.example.demo.model.Lista;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListaServiceImplTest {
    private ListaRepository listas;
    private FavoritoRepository favoritos;
    private UsuarioActual identidad;
    private ListaServiceImpl service;

    @BeforeEach
    void preparar() {
        listas = mock(ListaRepository.class);
        favoritos = mock(FavoritoRepository.class);
        identidad = mock(UsuarioActual.class);
        service = new ListaServiceImpl(listas, favoritos, identidad);
    }

    @Test
    void crearAsignaAlUsuarioAutenticadoNoAlCliente() {
        when(identidad.id()).thenReturn(42L);
        when(listas.guardar(any(Lista.class))).thenAnswer(call -> {
            Lista lista = call.getArgument(0);
            return new Lista(10L, lista.nombre(), lista.usuarioId());
        });

        var respuesta = service.crear(new ListaRequest("  Compras  "));

        assertThat(respuesta.usuarioId()).isEqualTo(42L);
        assertThat(respuesta.nombre()).isEqualTo("Compras");
        verify(listas).guardar(argThat(l -> l.id() == null && l.usuarioId().equals(42L)));
    }

    @Test
    void usuarioComunNoPuedeVerListaAjena() {
        when(identidad.id()).thenReturn(2L);
        when(listas.buscarPorId(9L)).thenReturn(Optional.of(new Lista(9L, "Ajena", 8L)));
        assertThatThrownBy(() -> service.obtenerPorId(9L)).isInstanceOf(AccesoProhibidoException.class);
    }

    @Test
    void moverValidaTambienPropietarioDelDestino() {
        when(identidad.id()).thenReturn(2L);
        when(listas.buscarPorId(1L)).thenReturn(Optional.of(new Lista(1L, "Mía", 2L)));
        when(listas.buscarPorId(3L)).thenReturn(Optional.of(new Lista(3L, "Ajena", 5L)));
        assertThatThrownBy(() -> service.moverFavoritos(1L, new MoverFavoritosRequest(3L)))
                .isInstanceOf(AccesoProhibidoException.class);
        verify(favoritos, never()).buscarPorListaId(anyLong());
    }
}
