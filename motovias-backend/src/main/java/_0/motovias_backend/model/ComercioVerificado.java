package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "comercios_verificados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComercioVerificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaComercio categoria;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(name = "fecha_alta", updatable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Misma convención que PuntoInteres.asignarFechaCreacion(): se fuerza la zona horaria
    // de Argentina explícitamente en vez de usar @CreationTimestamp, que tomaría UTC en Docker.
    @PrePersist
    private void alAlta() {
        LocalDateTime ahora = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDateTime();
        fechaAlta = ahora;
        fechaModificacion = ahora;
    }

    @PreUpdate
    private void alModificar() {
        fechaModificacion = ZonedDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDateTime();
    }
}
