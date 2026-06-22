package _0.motovias_backend.repository;

import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.model.ViajeParticipante;
import _0.motovias_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViajeParticipanteRepository extends JpaRepository<ViajeParticipante, Long> {

    long countByViaje(Viaje viaje);

    boolean existsByViajeAndUsuario(Viaje viaje, User usuario);
}
