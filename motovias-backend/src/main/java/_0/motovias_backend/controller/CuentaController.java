package _0.motovias_backend.controller;

import _0.motovias_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone las operaciones de derecho al olvido sobre la cuenta del usuario
 * autenticado. Separado de UserController para mantener el path literal
 * exigido por el criterio de aceptación (/api/usuarios/mi-cuenta) sin
 * duplicar el prefijo /api/users del resto del recurso.
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
}
