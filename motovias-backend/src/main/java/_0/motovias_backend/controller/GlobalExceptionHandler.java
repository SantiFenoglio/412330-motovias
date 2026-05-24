package _0.motovias_backend.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Manejo global de excepciones para la capa web.
 *
 * Por qué importa el orden de resolución:
 *  - ExceptionHandlerExceptionResolver (este @ControllerAdvice) tiene ORDER 0
 *  - ResponseStatusExceptionResolver tiene ORDER 1
 *
 * Si ResponseStatusException llega a ResponseStatusExceptionResolver, este llama
 * response.sendError() → Tomcat hace un error dispatch → la nueva request no
 * tiene headers CORS → el browser bloquea.
 * Al interceptarlo aquí primero devolvemos ResponseEntity, que escribe la
 * respuesta en el hilo actual conservando los CORS headers ya seteados por CorsFilter.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("message", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Conflicto de datos: " + message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error interno del servidor"));
    }
}
