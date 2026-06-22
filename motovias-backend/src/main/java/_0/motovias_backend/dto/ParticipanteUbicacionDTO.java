package _0.motovias_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteUbicacionDTO {

    private Long viajeId;
    private String email;
    private String nombre;
    private Double latitud;
    private Double longitud;
    // false cuando el usuario se desconecta; el frontend debe remover su marcador
    private boolean conectado;
}
