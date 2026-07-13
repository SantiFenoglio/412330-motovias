package _0.motovias_backend.controller;

import _0.motovias_backend.dto.MetricasResponseDTO;
import _0.motovias_backend.service.AdminMetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas agregadas de actividad de la plataforma para el dashboard de administración.
 * Protegido en SecurityConfig mediante hasRole("ADMIN") sobre /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin/metricas")
@RequiredArgsConstructor
public class AdminMetricasController {

    private final AdminMetricasService service;

    @GetMapping
    public ResponseEntity<MetricasResponseDTO> obtenerMetricas() {
        return ResponseEntity.ok(service.obtenerMetricas());
    }
}
