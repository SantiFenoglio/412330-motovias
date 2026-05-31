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
public class PuntoInteresResponseDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private double latitud;
    private double longitud;
    private Categoria categoria;
    private String emailUsuario;
}
