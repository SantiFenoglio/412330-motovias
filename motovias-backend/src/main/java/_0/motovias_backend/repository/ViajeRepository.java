package _0.motovias_backend.repository;

import _0.motovias_backend.model.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    Optional<Viaje> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
