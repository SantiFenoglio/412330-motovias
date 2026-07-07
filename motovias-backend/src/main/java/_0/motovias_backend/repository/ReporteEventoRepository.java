package _0.motovias_backend.repository;

import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteEvento;
import _0.motovias_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteEventoRepository extends JpaRepository<ReporteEvento, Long> {
    List<ReporteEvento> findByReporteOrderByTimestampDesc(PuntoInteres reporte);

    @Modifying
    @Query("DELETE FROM ReporteEvento e WHERE e.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") User usuario);
}
