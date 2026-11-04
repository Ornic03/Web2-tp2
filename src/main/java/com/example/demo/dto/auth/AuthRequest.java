package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "el email es obligatorio") @Email(message = "el email no es válido") String email,
        @NotBlank(message = "la contraseña es obligatoria") String password
) { }
