package _0.motovias_backend.repository;

import _0.motovias_backend.model.Notificacion;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.TipoNotificacion;
import _0.motovias_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioAndLeidaFalseOrderByFechaCreacionDesc(User destinatario);

    boolean existsByReporteAndDestinatarioAndTipoAndLeidaFalse(
            PuntoInteres reporte, User destinatario, TipoNotificacion tipo);

    List<Notificacion> findByReporteAndLeidaFalse(PuntoInteres reporte);

    Optional<Notificacion> findByIdAndDestinatario(Long id, User destinatario);

    @Modifying
    @Query("DELETE FROM Notificacion n WHERE n.destinatario = :destinatario")
    void deleteByDestinatario(@Param("destinatario") User destinatario);
}
