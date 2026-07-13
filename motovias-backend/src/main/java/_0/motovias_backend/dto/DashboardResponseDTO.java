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
public class DashboardResponseDTO {
    private long reportesActivos;
    private long votosRecibidos;
    private long caravanasParticipando;
    private List<AporteMensualDTO> aportesPorMes;
}
