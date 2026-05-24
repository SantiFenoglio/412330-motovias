package _0.motovias_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de prueba para validar el flujo completo de seguridad JWT.
 *
 * <p>El único endpoint ({@code GET /api/v1/test/protected}) está cubierto por
 * la regla {@code .anyRequest().authenticated()} de {@link _0.motovias_backend.security.SecurityConfig},
 * por lo que el {@link _0.motovias_backend.security.JwtAuthenticationFilter}
 * debe haber inyectado una autenticación válida antes de llegar aquí.
 *
 * <p>Casos de uso para Postman:
 * <ul>
 *   <li>Sin header → 401 Unauthorized</li>
 *   <li>Con {@code Authorization: Bearer <token válido>} → 200 OK</li>
 *   <li>Con token alterado/expirado → 401 Unauthorized</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    /**
     * Endpoint protegido. Solo accesible con un JWT válido en el header
     * {@code Authorization: Bearer <token>}.
     *
     * @param authentication objeto inyectado por Spring Security con el usuario autenticado.
     * @return 200 OK con el email del usuario autenticado.
     */
    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "Acceso autorizado",
                "user",    authentication.getName(),
                "status",  "OK"
        ));
    }
}
