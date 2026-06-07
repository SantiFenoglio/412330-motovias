package _0.motovias_backend.dto;

import _0.motovias_backend.model.TipoMotocicleta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    @NotBlank
    @Size(min = 3, message = "El nombre debe tener al menos 3 caracteres")
    private String nombre;

    private String apellido;
    private TipoMotocicleta tipoMotocicleta;
    private String tipoSangre;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String direccion;

    @Pattern.List({
        @Pattern(regexp = ".{8,}", message = "La contraseña debe tener al menos 8 caracteres"),
        @Pattern(regexp = ".*[A-Z].*", message = "La contraseña debe contener al menos una mayúscula"),
        @Pattern(regexp = ".*[a-z].*", message = "La contraseña debe contener al menos una minúscula"),
        @Pattern(regexp = ".*\\d.*", message = "La contraseña debe contener al menos un número"),
        @Pattern(regexp = ".*[@$!%*?&#+\\-_].*", message = "La contraseña debe contener al menos un carácter especial")
    })
    private String newPassword;
}
