package _0.motovias_backend.repository;

import _0.motovias_backend.model.ComercioVerificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComercioVerificadoRepository extends JpaRepository<ComercioVerificado, Long> {

    // Fuente de datos pública del mapa: solo comercios habilitados por el administrador.
    List<ComercioVerificado> findByActivoTrue();
}
