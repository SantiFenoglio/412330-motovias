package _0.motovias_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViajeResponseDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private String fechaCreacion;
    private String codigo;
    private String organizadorNombre;
    private String organizadorEmail;
}
