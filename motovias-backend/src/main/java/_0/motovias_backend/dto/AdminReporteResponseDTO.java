package _0.motovias_backend.dto;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReporteResponseDTO {
    private Long id;
    private String titulo;
    private Categoria categoria;
    private EstadoPunto estado;
    private String nombreUsuarioCreador;
    private String emailUsuarioCreador;
    private String fechaCreacion;
    private long votos;
}
