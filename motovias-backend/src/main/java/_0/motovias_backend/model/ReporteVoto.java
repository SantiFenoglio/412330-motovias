package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "reporte_votos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_usuario_reporte",
        columnNames = {"usuario_id", "reporte_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporte_id", nullable = false)
    private PuntoInteres reporte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVoto tipoVoto;
}
