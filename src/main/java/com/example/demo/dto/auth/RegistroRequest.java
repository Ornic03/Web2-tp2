package com.example.demo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "el email es obligatorio") @Email(message = "el email no es válido") String email,
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "la contraseña debe tener entre 8 y 72 caracteres") String password
) { }
