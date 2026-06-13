package _0.motovias_backend.controller;

import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<ReporteResponseDTO>> listarMios(Authentication authentication) {
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(service.listarMios(usuario));
    }

    @PostMapping
    public ResponseEntity<ReporteResponseDTO> crear(
            @Valid @RequestBody ReporteRequestDTO dto,
            Authentication authentication
    ) {
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        ReporteResponseDTO reporte = service.crear(dto, usuario);
        messagingTemplate.convertAndSend("/topic/reportes", reporte);

        return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
    }
}
