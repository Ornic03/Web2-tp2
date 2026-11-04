package com.example.demo.dto.auth;

public record LoginResponse(String token, String tipo, long expiraEnSegundos) { }
