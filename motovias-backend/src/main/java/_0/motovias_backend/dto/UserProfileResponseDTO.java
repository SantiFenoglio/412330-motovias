package _0.motovias_backend.dto;

import _0.motovias_backend.model.TipoMotocicleta;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponseDTO {
    private String nombre;
    private String apellido;
    private String email;
    private TipoMotocicleta tipoMotocicleta;
    private boolean activo;
    private String role;
    private String tipoSangre;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String direccion;
}
