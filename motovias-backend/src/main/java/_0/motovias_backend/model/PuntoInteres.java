package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

/**
 * Representa un punto de interés geoposicionado en el mapa.
 *
 * El campo {@code ubicacion} usa el tipo JTS {@link Point} mapeado por Hibernate Spatial
 * al tipo PostGIS {@code geometry(Point,4326)} en PostgreSQL.
 * SRID 4326 = WGS84, compatible con las coordenadas GPS del navegador (Leaflet/OSM).
 *
 * Para crear instancias con el SRID correcto:
 * <pre>
 *   GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
 *   Point p = gf.createPoint(new Coordinate(longitud, latitud));
 * </pre>
 */
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

    /**
     * Ubicación geográfica en SRID 4326 (WGS84).
     * columnDefinition declara el tipo PostGIS exacto para que Hibernate genere
     * la columna con restricción de geometría y sistema de referencia espacial.
     */
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point ubicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;
}
