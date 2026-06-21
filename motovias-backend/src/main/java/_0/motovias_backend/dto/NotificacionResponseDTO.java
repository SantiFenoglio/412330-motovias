package _0.motovias_backend.dto;

import _0.motovias_backend.model.TipoNotificacion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificacionResponseDTO {
    private Long id;
    private String mensaje;
    private String fechaCreacion;
    private boolean leida;
    private TipoNotificacion tipo;
    private Long reporteId;
    private String reporteTitulo;
}
