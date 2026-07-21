package _0.motovias_backend.repository;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.projection.AporteMensualProjection;
import _0.motovias_backend.repository.projection.CategoriaConteoProjection;
import _0.motovias_backend.repository.projection.ZonaActividadProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PuntoInteresRepository extends JpaRepository<PuntoInteres, Long>,
        JpaSpecificationExecutor<PuntoInteres> {

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
            AND estado <> 'ELIMINADO'
            """, nativeQuery = true)
    List<PuntoInteres> findCercanos(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radioMetros") double radioMetros
    );

    // Usado únicamente por la baja de cuenta (UserService.eliminarCuenta) para purgar
    // físicamente todos los reportes del usuario, incluidos los ya dados de baja lógica.
    List<PuntoInteres> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    // "Mis Publicaciones": listado personal del usuario, excluyendo los que dio de baja lógica.
    List<PuntoInteres> findByUsuarioIdAndEstadoNotOrderByFechaCreacionDesc(Long usuarioId, EstadoPunto estado);

    // Fuente de datos pública del mapa: los reportes dados de baja lógica nunca deben renderizarse.
    List<PuntoInteres> findByEstadoNot(EstadoPunto estado);

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

    // Dashboard de métricas — total de reportes vigentes (todo lo que no está dado de baja lógica).
    long countByEstadoNot(EstadoPunto estadoExcluido);

    // Dashboard de métricas — reportes creados en los últimos 7 días, excluyendo los dados de baja.
    long countByFechaCreacionAfterAndEstadoNot(LocalDateTime desde, EstadoPunto estadoExcluido);

    // Dashboard de métricas — conteo agrupado por categoría (JPQL, portable entre H2 y PostgreSQL).
    @Query("""
            SELECT p.categoria AS categoria, COUNT(p) AS cantidad
            FROM PuntoInteres p
            WHERE p.estado <> :estadoExcluido
            GROUP BY p.categoria
            """)
    List<CategoriaConteoProjection> countPorCategoria(@Param("estadoExcluido") EstadoPunto estadoExcluido);

    /**
     * Dashboard de métricas — "Zonas con más actividad": agrupa los reportes por celda de
     * cuadrícula geoespacial usando ST_SnapToGrid (0.01 grados ≈ 1.1 km) y devuelve, por cada
     * celda, el centroide (ST_Centroid) de los puntos agrupados junto con su conteo.
     *
     * Consulta nativa exclusiva de PostGIS: no soportada por H2 (perfil de test), donde se
     * omite mediante manejo defensivo en el servicio. Validada contra PostgreSQL real vía Postman.
     */
    @Query(value = """
            SELECT
                ST_Y(ST_Centroid(ST_Collect(ubicacion))) AS latitud,
                ST_X(ST_Centroid(ST_Collect(ubicacion))) AS longitud,
                COUNT(*) AS cantidad
            FROM puntos_interes
            WHERE estado <> 'ELIMINADO'
            GROUP BY ST_SnapToGrid(ubicacion, 0.01)
            ORDER BY cantidad DESC
            LIMIT 10
            """, nativeQuery = true)
    List<ZonaActividadProjection> findZonasConMasActividad();

    // Dashboard personal — total de reportes del usuario en un estado dado (ej. ACTIVO).
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoPunto estado);

    // Dashboard personal — reportes creados por el usuario agrupados por año/mes, para
    // graficar su evolución de aportes. YEAR()/MONTH() son funciones HQL portables entre
    // H2 (perfil de test) y PostgreSQL.
    @Query("""
            SELECT YEAR(p.fechaCreacion) AS anio, MONTH(p.fechaCreacion) AS mes, COUNT(p) AS cantidad
            FROM PuntoInteres p
            WHERE p.usuario = :usuario AND p.fechaCreacion >= :desde
            GROUP BY YEAR(p.fechaCreacion), MONTH(p.fechaCreacion)
            ORDER BY YEAR(p.fechaCreacion), MONTH(p.fechaCreacion)
            """)
    List<AporteMensualProjection> countMensualPorUsuario(
            @Param("usuario") User usuario,
            @Param("desde") LocalDateTime desde);
}
