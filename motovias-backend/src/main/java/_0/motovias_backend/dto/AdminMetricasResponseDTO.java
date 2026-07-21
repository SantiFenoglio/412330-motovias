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
public class AdminMetricasResponseDTO {
    private long totalReportesActivos;
    private long reportesEstaSemana;
    private long usuariosNuevosEsteMes;
    private List<CategoriaConteoDTO> reportesPorCategoria;
    private List<ZonaActividadDTO> zonasMasActivas;
}
