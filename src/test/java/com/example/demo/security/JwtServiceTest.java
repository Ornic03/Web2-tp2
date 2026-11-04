package com.example.demo.security;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    @Test
    void elTokenFirmadoContieneIdYRol() {
        var service = new JwtService("clave-de-pruebas-segura-con-mas-de-32-bytes-2026", 60);
        var jwt = service.crear(new Usuario(12L, "persona@ejemplo.com", "hash", Rol.ADMIN));
        assertThat(service.validar(jwt)).isEqualTo(new UsuarioAutenticado(12L, Rol.ADMIN));
    }

    @Test
    void tokenDeOtraClaveNoSeAcepta() {
        var emisor = new JwtService("clave-emisora-super-segura-de-test-2026", 60);
        var receptor = new JwtService("clave-receptora-super-segura-test-2026", 60);
        var token = emisor.crear(new Usuario(12L, "p@ejemplo.com", "hash", Rol.USER));
        assertThatThrownBy(() -> receptor.validar(token)).isInstanceOf(JwtException.class);
    }
}
