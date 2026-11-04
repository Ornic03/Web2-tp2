package com.example.demo.security;

/** Puerto para la identidad del request, fácil de simular en tests unitarios. */
public interface UsuarioActual {
    Long id();
    boolean esAdmin();
}
