package _0.motovias_backend.dto;

import _0.motovias_backend.model.CategoriaComercio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComercioVerificadoResponseDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;
    private CategoriaComercio categoria;
    private double latitud;
    private double longitud;
    private boolean activo;
    private boolean verificado;
    private String fechaAlta;
    private String fechaModificacion;
}
