package com.example.demo.service;

import com.example.demo.dto.auth.RegistroRequest;
import com.example.demo.exception.ConflictoException;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceImplTest {
    @Test
    void registrarGuardaHashBCryptYNoContraseñaPlana() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        JwtService jwt = mock(JwtService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        when(repo.guardar(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return new Usuario(15L, u.email(), u.passwordHash(), u.rol());
        });
        var servicio = new UsuarioServiceImpl(repo, encoder, jwt);

        var resultado = servicio.registrar(new RegistroRequest("  PERSONA@ejemplo.com ", "ClaveSegura123"));

        assertThat(resultado.email()).isEqualTo("persona@ejemplo.com");
        assertThat(resultado.rol().name()).isEqualTo("USER");
        verify(repo).guardar(argThat(u -> !u.passwordHash().equals("ClaveSegura123")
                && u.passwordHash().startsWith("$2") && encoder.matches("ClaveSegura123", u.passwordHash())));
    }

    @Test
    void emailDuplicadoGeneraConflicto() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.existePorEmail("persona@ejemplo.com")).thenReturn(true);
        var servicio = new UsuarioServiceImpl(repo, new BCryptPasswordEncoder(), mock(JwtService.class));
        assertThatThrownBy(() -> servicio.registrar(new RegistroRequest("persona@ejemplo.com", "ClaveSegura123")))
                .isInstanceOf(ConflictoException.class);
        verify(repo, never()).guardar(any());
    }
}
