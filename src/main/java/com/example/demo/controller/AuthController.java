package com.example.demo.controller;

import com.example.demo.dto.auth.*;
import com.example.demo.service.UsuarioServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro y login públicos: el login devuelve un JWT Bearer")
public class AuthController {
    private final UsuarioServiceImpl usuarios;
    public AuthController(UsuarioServiceImpl usuarios) { this.usuarios = usuarios; }

    @PostMapping("/registro")
    @Operation(summary = "Registrar usuario", description = "Crea un USER con contraseña hasheada; 409 si el email existe")
    public ResponseEntity<RegistroResponse> registro(@Valid @RequestBody RegistroRequest request) {
        RegistroResponse creado = usuarios.registrar(request);
        return ResponseEntity.status(201).body(creado);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Devuelve JWT firmado; email/contraseña incorrectos: 401")
    public LoginResponse login(@Valid @RequestBody AuthRequest request) { return usuarios.login(request); }
}
