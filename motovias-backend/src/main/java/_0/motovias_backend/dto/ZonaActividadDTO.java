package _0.motovias_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaActividadDTO {
    private Double latitud;
    private Double longitud;
    private long cantidad;
}
