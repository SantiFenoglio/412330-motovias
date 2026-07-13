package _0.motovias_backend.dto;

import _0.motovias_backend.model.Categoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaConteoDTO {
    private Categoria categoria;
    private long cantidad;
}
