package _0.motovias_backend.repository;

import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.model.ViajeParticipante;
import _0.motovias_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ViajeParticipanteRepository extends JpaRepository<ViajeParticipante, Long> {

    long countByViaje(Viaje viaje);

    // Dashboard personal — cantidad de caravanas en las que el usuario participa.
    long countByUsuario(User usuario);

    boolean existsByViajeAndUsuario(Viaje viaje, User usuario);

    Optional<ViajeParticipante> findByViajeAndUsuario(Viaje viaje, User usuario);

    Optional<ViajeParticipante> findTopByUsuarioOrderByFechaUnionDesc(User usuario);

    @Query("SELECT vp FROM ViajeParticipante vp JOIN FETCH vp.usuario WHERE vp.viaje = :viaje")
    List<ViajeParticipante> findByViajeWithUsuario(@Param("viaje") Viaje viaje);

    @Modifying
    @Query("DELETE FROM ViajeParticipante vp WHERE vp.viaje = :viaje")
    void deleteByViaje(@Param("viaje") Viaje viaje);

    @Modifying
    @Query("DELETE FROM ViajeParticipante vp WHERE vp.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") User usuario);
}
