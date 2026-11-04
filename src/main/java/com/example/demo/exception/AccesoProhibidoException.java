package com.example.demo.exception;

public class AccesoProhibidoException extends RuntimeException {
    public AccesoProhibidoException(String mensaje) { super(mensaje); }
}
