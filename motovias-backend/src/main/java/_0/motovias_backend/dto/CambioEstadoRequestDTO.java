package _0.motovias_backend.dto;

import _0.motovias_backend.model.EstadoPunto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambioEstadoRequestDTO {

    @NotNull(message = "El estado es obligatorio")
    private EstadoPunto estado;
}
