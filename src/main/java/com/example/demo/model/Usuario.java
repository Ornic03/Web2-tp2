package com.example.demo.model;

public record Usuario(Long id, String email, String passwordHash, Rol rol) { }
