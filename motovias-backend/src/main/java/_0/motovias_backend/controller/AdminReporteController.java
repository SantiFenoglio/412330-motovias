package _0.motovias_backend.controller;

import _0.motovias_backend.dto.AdminReporteResponseDTO;
import _0.motovias_backend.dto.CambioEstadoRequestDTO;
import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.AdminReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Endpoints de administración y moderación de reportes.
 * Protegidos en SecurityConfig mediante hasRole("ADMIN") sobre /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final AdminReporteService service;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<AdminReporteResponseDTO>> listar(
            @RequestParam(required = false) EstadoPunto estado,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Pageable pageable
    ) {
        LocalDateTime desde = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime hasta = fechaFin != null ? LocalDateTime.of(fechaFin, LocalTime.MAX) : null;

        return ResponseEntity.ok(service.listar(estado, categoria, desde, hasta, pageable));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<AdminReporteResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequestDTO dto,
            Authentication authentication
    ) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return ResponseEntity.ok(service.cambiarEstado(id, dto.getEstado(), admin));
    }
}
