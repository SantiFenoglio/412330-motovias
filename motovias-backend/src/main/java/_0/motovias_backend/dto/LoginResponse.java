package _0.motovias_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor   // requerido por Jackson para deserializar desde JSON
@AllArgsConstructor  // usado internamente en AuthService al construir la respuesta
public class LoginResponse {
    private String token;
    private String email;
    private String role;
}
