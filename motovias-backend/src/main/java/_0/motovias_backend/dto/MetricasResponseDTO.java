package _0.motovias_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasResponseDTO {
    private long totalActivos;
    private long reportesUltimaSemana;
    private long usuariosNuevosMes;
    private List<CategoriaConteoDTO> reportesPorCategoria;
    private List<ZonaActividadDTO> zonasConMasActividad;
}
