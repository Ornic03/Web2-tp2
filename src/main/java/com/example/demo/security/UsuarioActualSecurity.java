package com.example.demo.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActualSecurity implements UsuarioActual {
    private UsuarioAutenticado usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AuthenticationCredentialsNotFoundException("Se necesita iniciar sesión");
        }
        return usuario;
    }

    @Override
    public Long id() { return usuario().id(); }

    @Override
    public boolean esAdmin() { return usuario().rol() == com.example.demo.model.Rol.ADMIN; }
}
