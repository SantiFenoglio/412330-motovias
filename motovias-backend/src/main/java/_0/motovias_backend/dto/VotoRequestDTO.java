package _0.motovias_backend.dto;

import _0.motovias_backend.model.TipoVoto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VotoRequestDTO {

    @NotNull(message = "El tipo de voto es obligatorio")
    private TipoVoto tipoVoto;
}
