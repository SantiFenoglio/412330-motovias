package _0.motovias_backend.repository;

import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteVoto;
import _0.motovias_backend.model.TipoVoto;
import _0.motovias_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReporteVotoRepository extends JpaRepository<ReporteVoto, Long> {
    Optional<ReporteVoto> findByUsuarioAndReporte(User usuario, PuntoInteres reporte);
    long countByReporteAndTipoVoto(PuntoInteres reporte, TipoVoto tipoVoto);
}
