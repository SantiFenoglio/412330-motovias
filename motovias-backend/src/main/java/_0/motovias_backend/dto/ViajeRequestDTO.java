package _0.motovias_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ViajeRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;
}
