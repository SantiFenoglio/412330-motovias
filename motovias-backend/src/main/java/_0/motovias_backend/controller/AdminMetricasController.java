package _0.motovias_backend.controller;

import _0.motovias_backend.dto.AdminMetricasResponseDTO;
import _0.motovias_backend.service.AdminMetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas agregadas de actividad de la plataforma para el dashboard de administración.
 * Doble protección: SecurityConfig ya restringe /api/admin/** a ROLE_ADMIN a nivel de URL,
 * y @PreAuthorize a nivel de método deja la intención explícita en el propio controlador.
 */
@RestController
@RequestMapping("/api/admin/metricas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricasController {

    private final AdminMetricasService service;

    @GetMapping
    public ResponseEntity<AdminMetricasResponseDTO> obtenerMetricas() {
        return ResponseEntity.ok(service.obtenerMetricas());
    }
}
