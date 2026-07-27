package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "fotos_reporte")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporte_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PuntoInteres reporte;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "fecha_carga", updatable = false)
    private LocalDateTime fechaCarga;

    // Ver comentario equivalente en PuntoInteres: se evita @CreationTimestamp de
    // Hibernate porque toma el reloj por defecto de la JVM (UTC en Docker) en vez
    // de la hora de pared de Argentina.
    @PrePersist
    private void asignarFechaCarga() {
        fechaCarga = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDateTime();
    }
}
