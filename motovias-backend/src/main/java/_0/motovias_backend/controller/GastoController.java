package _0.motovias_backend.controller;

import _0.motovias_backend.dto.GastoRequestDTO;
import _0.motovias_backend.dto.GastoResponseDTO;
import _0.motovias_backend.dto.PreferenciaPagoRequestDTO;
import _0.motovias_backend.dto.PreferenciaPagoResponseDTO;
import _0.motovias_backend.dto.TransferenciaSimplificadaDTO;
import _0.motovias_backend.service.GastoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viajes/{viajeId}/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;

    @PostMapping
    public ResponseEntity<GastoResponseDTO> registrarGasto(
            @PathVariable Long viajeId,
            @Valid @RequestBody GastoRequestDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gastoService.registrarGasto(viajeId, dto, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<GastoResponseDTO>> listarGastos(@PathVariable Long viajeId) {
        return ResponseEntity.ok(gastoService.listarGastos(viajeId));
    }

    @GetMapping("/balance")
    public ResponseEntity<List<TransferenciaSimplificadaDTO>> calcularBalance(@PathVariable Long viajeId) {
        return ResponseEntity.ok(gastoService.calcularBalance(viajeId));
    }

    @PostMapping("/preferencia")
    public ResponseEntity<PreferenciaPagoResponseDTO> crearPreferenciaPago(
            @PathVariable Long viajeId,
            @Valid @RequestBody PreferenciaPagoRequestDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                gastoService.crearPreferenciaPago(viajeId, dto, authentication.getName()));
    }
}
