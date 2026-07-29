package _0.motovias_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambioEstadoComercioRequestDTO {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;
}
