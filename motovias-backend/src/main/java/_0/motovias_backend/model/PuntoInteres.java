package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "puntos_interes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuntoInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVO'")
    private EstadoPunto estado = EstadoPunto.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "fuente_ubicacion", nullable = false, columnDefinition = "varchar(255) default 'GPS'")
    private FuenteUbicacion fuenteUbicacion = FuenteUbicacion.GPS;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;
}
