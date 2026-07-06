package _0.motovias_backend.dto;

import _0.motovias_backend.model.CategoriaGasto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GastoResponseDTO {
    private Long id;
    private String descripcion;
    private BigDecimal monto;
    private CategoriaGasto categoria;
    private String fechaCreacion;
    private String pagadorNombre;
    private String pagadorEmail;
}
