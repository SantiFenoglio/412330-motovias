package _0.motovias_backend.controller;

import _0.motovias_backend.dto.PuntoInteresRequestDTO;
import _0.motovias_backend.dto.PuntoInteresResponseDTO;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.PuntoInteresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/puntos-interes")
@RequiredArgsConstructor
public class PuntoInteresController {

    private final PuntoInteresService service;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<PuntoInteresResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PuntoInteresResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Busca puntos de interés dentro del radio dado en metros.
     *
     * @param lat        Latitud WGS84 del centro de búsqueda
     * @param lon        Longitud WGS84 del centro de búsqueda
     * @param radio      Radio de búsqueda en metros (default: 1000 m)
     */
    @GetMapping("/cercanos")
    public ResponseEntity<List<PuntoInteresResponseDTO>> cercanos(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1000") double radio
    ) {
        return ResponseEntity.ok(service.buscarCercanos(lat, lon, radio));
    }

    @PostMapping
    public ResponseEntity<PuntoInteresResponseDTO> crear(
            @RequestBody PuntoInteresRequestDTO dto,
            Authentication authentication
    ) {
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, usuario));
    }
}
