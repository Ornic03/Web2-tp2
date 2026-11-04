package com.example.demo.dto.auth;

import com.example.demo.model.Rol;

public record RegistroResponse(Long id, String email, Rol rol) { }
