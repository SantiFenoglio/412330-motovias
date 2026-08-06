package _0.motovias_backend.controller;

import _0.motovias_backend.dto.CambioEstadoComercioRequestDTO;
import _0.motovias_backend.dto.ComercioVerificadoRequestDTO;
import _0.motovias_backend.dto.ComercioVerificadoResponseDTO;
import _0.motovias_backend.service.ComercioVerificadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión administrativa de comercios verificados (talleres y gomerías).
 * Doble protección: SecurityConfig ya restringe /api/admin/** a ROLE_ADMIN a nivel de URL,
 * y @PreAuthorize a nivel de método deja la intención explícita en el propio controlador.
 */
@RestController
@RequestMapping("/api/admin/comercios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComercioController {

    private final ComercioVerificadoService service;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<ComercioVerificadoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    public ResponseEntity<ComercioVerificadoResponseDTO> crear(
            @Valid @RequestBody ComercioVerificadoRequestDTO dto
    ) {
        ComercioVerificadoResponseDTO comercio = service.crear(dto);
        messagingTemplate.convertAndSend("/topic/comercios", comercio);
        return ResponseEntity.status(HttpStatus.CREATED).body(comercio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComercioVerificadoResponseDTO> editar(
            @PathVariable Long id,
            @Valid @RequestBody ComercioVerificadoRequestDTO dto
    ) {
        ComercioVerificadoResponseDTO comercio = service.editar(id, dto);
        messagingTemplate.convertAndSend("/topic/comercios", comercio);
        return ResponseEntity.ok(comercio);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ComercioVerificadoResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoComercioRequestDTO dto
    ) {
        ComercioVerificadoResponseDTO comercio = service.cambiarEstado(id, dto.getActivo());
        messagingTemplate.convertAndSend("/topic/comercios", comercio);
        return ResponseEntity.ok(comercio);
    }
}
