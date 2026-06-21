package _0.motovias_backend.repository;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PuntoInteresRepository extends JpaRepository<PuntoInteres, Long> {

    /**
     * Devuelve todos los puntos de interés cuya ubicación esté dentro del radio dado.
     *
     * ST_DWithin sobre geografía usa metros como unidad de distancia y tiene en
     * cuenta la curvatura terrestre, lo que da resultados precisos para radios
     * urbanos típicos (100 m – 50 km).
     *
     * El cast ::geography convierte la geometría SRID-4326 a tipo geography de
     * PostGIS sin reproyección; ST_MakePoint(lon, lat) construye el punto de
     * búsqueda respetando el orden (X=longitud, Y=latitud) del estándar WKT/WGS84.
     */
    @Query(value = """
            SELECT * FROM puntos_interes
            WHERE ST_DWithin(
                ubicacion::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radioMetros
            )
            """, nativeQuery = true)
    List<PuntoInteres> findCercanos(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radioMetros") double radioMetros
    );

    List<PuntoInteres> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @Query("""
            SELECT p FROM PuntoInteres p LEFT JOIN FETCH p.usuario
            WHERE p.categoria = :categoria
              AND p.estado = :estado
              AND p.fechaCreacion < :limite
            """)
    List<PuntoInteres> findAlertasSosVencidas(
            @Param("categoria") Categoria categoria,
            @Param("estado") EstadoPunto estado,
            @Param("limite") LocalDateTime limite);
}
