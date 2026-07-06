package _0.motovias_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferenciaSimplificadaDTO {
    private String deudorEmail;
    private String deudorNombre;
    private String acreedorEmail;
    private String acreedorNombre;
    private BigDecimal monto;
}
