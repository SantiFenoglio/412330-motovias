package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "viaje_participantes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_viaje_usuario",
                columnNames = {"viaje_id", "usuario_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViajeParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @CreationTimestamp
    @Column(name = "fecha_union", updatable = false)
    private LocalDateTime fechaUnion;
}
