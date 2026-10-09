package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de errores de toda la API. En vez de que cada
 * controller arme su propio JSON de error, cualquier excepción que llegue
 * hasta acá se traduce a un mismo formato (ProblemDetail, RFC 7807):
 * { "status", "title", "detail", ... }.
 *
 * Cuando se implemente el controller de favoritos en clase, no hace falta
 * tocar esta clase: alcanza con que FavoritoService lance
 * RecursoNoEncontradoException o que el DTO de entrada tenga anotaciones
 * de Bean Validation para que estos mismos handlers respondan.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail handleServicioExterno(ServicioExternoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problema.setTitle("Falla al consumir un servicio externo");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos");
        problema.setTitle("Error de validación");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail handleConflicto(ConflictoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridad(org.springframework.dao.DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operación entra en conflicto con los datos relacionados. Revisá las listas y sus favoritos.");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail handleSolicitud(SolicitudInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleFormato(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El JSON o el identificador no tiene un formato válido");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
        problema.setTitle("Error interno");
        return problema;
    }
}
