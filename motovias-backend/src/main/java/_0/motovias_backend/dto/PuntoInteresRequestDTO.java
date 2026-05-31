package _0.motovias_backend.dto;

import _0.motovias_backend.model.Categoria;
import lombok.Data;

@Data
public class PuntoInteresRequestDTO {
    private String titulo;
    private String descripcion;
    private double latitud;
    private double longitud;
    private Categoria categoria;
}
