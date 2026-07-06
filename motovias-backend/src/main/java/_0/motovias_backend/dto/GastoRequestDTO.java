package _0.motovias_backend.dto;

import _0.motovias_backend.model.CategoriaGasto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GastoRequestDTO {

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 17, fraction = 2, message = "Máximo 2 decimales")
    private BigDecimal monto;

    @NotNull(message = "La categoría es obligatoria")
    private CategoriaGasto categoria;

    /** Si es null, se usa el usuario autenticado como pagador. */
    private Long pagadorId;
}
