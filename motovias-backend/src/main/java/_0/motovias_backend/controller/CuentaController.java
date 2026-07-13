package _0.motovias_backend.controller;

import _0.motovias_backend.dto.DashboardResponseDTO;
import _0.motovias_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone operaciones sobre la cuenta del usuario autenticado (derecho al
 * olvido, dashboard de actividad personal). Separado de UserController para
 * mantener el prefijo literal /api/usuarios exigido por los criterios de
 * aceptación sin duplicar el prefijo /api/users del resto del recurso.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class CuentaController {

    private final UserService userService;

    @DeleteMapping("/mi-cuenta")
    public ResponseEntity<Void> eliminarCuenta() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.eliminarCuenta(email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mi-dashboard")
    public ResponseEntity<DashboardResponseDTO> obtenerMiDashboard() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userService.obtenerMiDashboard(email));
    }
}
