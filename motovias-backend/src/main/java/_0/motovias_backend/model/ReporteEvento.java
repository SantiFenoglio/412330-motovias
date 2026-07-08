package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "reporte_eventos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporte_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PuntoInteres reporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEventoReporte tipoEvento;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Ver comentario equivalente en PuntoInteres: se evita @CreationTimestamp de
    // Hibernate porque toma el reloj por defecto de la JVM (UTC en Docker) en vez
    // de la hora de pared de Argentina.
    @PrePersist
    private void asignarTimestamp() {
        timestamp = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDateTime();
    }
}
