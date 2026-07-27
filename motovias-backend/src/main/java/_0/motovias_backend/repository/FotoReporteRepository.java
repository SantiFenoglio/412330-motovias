package _0.motovias_backend.repository;

import _0.motovias_backend.model.FotoReporte;
import _0.motovias_backend.model.PuntoInteres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoReporteRepository extends JpaRepository<FotoReporte, Long> {

    List<FotoReporte> findByReporteOrderByFechaCargaAsc(PuntoInteres reporte);

    long countByReporte(PuntoInteres reporte);
}
