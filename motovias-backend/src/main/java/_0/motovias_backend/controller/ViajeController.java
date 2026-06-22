package _0.motovias_backend.controller;

import _0.motovias_backend.dto.ViajeRequestDTO;
import _0.motovias_backend.dto.ViajeResponseDTO;
import _0.motovias_backend.service.ViajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private final ViajeService viajeService;

    @PostMapping
    public ResponseEntity<ViajeResponseDTO> crearViaje(
            @Valid @RequestBody ViajeRequestDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(viajeService.crearViaje(dto, authentication.getName()));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ViajeResponseDTO> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(viajeService.buscarPorCodigo(codigo));
    }

    @PostMapping("/{codigo}/unirse")
    public ResponseEntity<ViajeResponseDTO> unirseAViaje(
            @PathVariable String codigo,
            Authentication authentication
    ) {
        return ResponseEntity.ok(viajeService.unirseAViaje(codigo, authentication.getName()));
    }
}
