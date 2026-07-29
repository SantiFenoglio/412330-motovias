package _0.motovias_backend.controller;

import _0.motovias_backend.dto.ComercioVerificadoResponseDTO;
import _0.motovias_backend.service.ComercioVerificadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consumo público de comercios verificados para la capa del mapa.
 */
@RestController
@RequestMapping("/api/comercios")
@RequiredArgsConstructor
public class ComercioController {

    private final ComercioVerificadoService service;

    @GetMapping("/activos")
    public ResponseEntity<List<ComercioVerificadoResponseDTO>> listarActivos() {
        return ResponseEntity.ok(service.listarActivos());
    }
}
