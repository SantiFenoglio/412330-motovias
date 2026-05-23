package _0.motovias_backend.dto;

import _0.motovias_backend.model.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private Role role;
    private String tipoMotocicleta;
}
