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
public class ReporteResponseDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private double latitud;
    private double longitud;
    private Categoria categoria;
    private EstadoPunto estado;
    private String fechaCreacion;
    private String emailUsuario;
    private String nombreUsuario;
}
