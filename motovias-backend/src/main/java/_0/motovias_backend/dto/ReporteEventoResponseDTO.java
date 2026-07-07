package _0.motovias_backend.dto;

import _0.motovias_backend.model.TipoEventoReporte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEventoResponseDTO {
    private Long id;
    private TipoEventoReporte tipoEvento;
    private String descripcion;
    private String nombreUsuario;
    private String timestamp;
}
