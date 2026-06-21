package _0.motovias_backend.controller;

import _0.motovias_backend.dto.NotificacionResponseDTO;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listarNoLeidas(Authentication authentication) {
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(notificacionService.listarNoLeidas(usuario));
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponseDTO> marcarLeida(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User usuario = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(notificacionService.marcarLeida(id, usuario));
    }
}
