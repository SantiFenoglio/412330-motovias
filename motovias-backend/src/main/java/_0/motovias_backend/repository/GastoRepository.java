package _0.motovias_backend.repository;

import _0.motovias_backend.model.Gasto;
import _0.motovias_backend.model.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @Query("SELECT g FROM Gasto g JOIN FETCH g.pagador WHERE g.viaje = :viaje ORDER BY g.fechaCreacion DESC")
    List<Gasto> findByViajeOrderByFechaCreacionDesc(@Param("viaje") Viaje viaje);

    @Query("SELECT g FROM Gasto g JOIN FETCH g.pagador WHERE g.viaje = :viaje")
    List<Gasto> findAllByViaje(@Param("viaje") Viaje viaje);

    @Modifying
    @Query("DELETE FROM Gasto g WHERE g.viaje = :viaje")
    void deleteByViaje(@Param("viaje") Viaje viaje);
}
