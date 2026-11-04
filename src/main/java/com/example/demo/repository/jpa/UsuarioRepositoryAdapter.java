package com.example.demo.repository.jpa;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class UsuarioRepositoryAdapter implements UsuarioRepository {
    private final UsuarioJpaRepository jpa;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        var entity = new UsuarioEntity(usuario.id(), usuario.email(), usuario.passwordHash(), usuario.rol());
        return mapear(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) { return jpa.findByEmail(email).map(this::mapear); }

    @Override
    public Optional<Usuario> buscarPorId(Long id) { return jpa.findById(id).map(this::mapear); }

    @Override
    public boolean existePorEmail(String email) { return jpa.existsByEmail(email); }

    private Usuario mapear(UsuarioEntity e) { return new Usuario(e.getId(), e.getEmail(), e.getPasswordHash(), e.getRol()); }
}
