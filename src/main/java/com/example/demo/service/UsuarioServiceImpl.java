package com.example.demo.service;

import com.example.demo.dto.auth.*;
import com.example.demo.exception.ConflictoException;
import com.example.demo.exception.CredencialesInvalidasException;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class UsuarioServiceImpl {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public UsuarioServiceImpl(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public RegistroResponse registrar(RegistroRequest request) {
        String email = normalizar(request.email());
        if (usuarios.existePorEmail(email)) throw new ConflictoException("El email ya está registrado");
        Usuario nuevo = usuarios.guardar(new Usuario(null, email, encoder.encode(request.password()), Rol.USER));
        return new RegistroResponse(nuevo.id(), nuevo.email(), nuevo.rol());
    }

    public LoginResponse login(AuthRequest request) {
        String email = normalizar(request.email());
        Usuario usuario = usuarios.buscarPorEmail(email).orElseThrow(CredencialesInvalidasException::new);
        if (!encoder.matches(request.password(), usuario.passwordHash())) throw new CredencialesInvalidasException();
        return new LoginResponse(jwt.crear(usuario), "Bearer", jwt.expiracionSegundos());
    }

    private String normalizar(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
