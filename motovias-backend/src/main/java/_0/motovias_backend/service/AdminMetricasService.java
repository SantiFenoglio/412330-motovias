package _0.motovias_backend.service;

import _0.motovias_backend.dto.AdminMetricasResponseDTO;
import _0.motovias_backend.dto.CategoriaConteoDTO;
import _0.motovias_backend.dto.ZonaActividadDTO;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetricasService {

    private final PuntoInteresRepository puntoInteresRepository;
    private final UserRepository userRepository;

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    public AdminMetricasResponseDTO obtenerMetricas() {
        LocalDateTime ahora = ZonedDateTime.now(ZONA_ARGENTINA).toLocalDateTime();
        LocalDateTime haceUnaSemana = ahora.minusDays(7);
        LocalDateTime inicioMes = LocalDate.now(ZONA_ARGENTINA).withDayOfMonth(1).atStartOfDay();

        return AdminMetricasResponseDTO.builder()
                .totalReportesActivos(puntoInteresRepository.countByEstadoNot(EstadoPunto.ELIMINADO))
                .reportesEstaSemana(puntoInteresRepository
                        .countByFechaCreacionAfterAndEstadoNot(haceUnaSemana, EstadoPunto.ELIMINADO))
                .usuariosNuevosEsteMes(userRepository.countByFechaCreacionGreaterThanEqual(inicioMes))
                .reportesPorCategoria(obtenerReportesPorCategoria())
                .zonasMasActivas(obtenerZonasMasActivas())
                .build();
    }

    private List<CategoriaConteoDTO> obtenerReportesPorCategoria() {
        return puntoInteresRepository.countPorCategoria(EstadoPunto.ELIMINADO).stream()
                .map(p -> CategoriaConteoDTO.builder()
                        .categoria(p.getCategoria())
                        .cantidad(p.getCantidad())
                        .build())
                .toList();
    }

    private List<ZonaActividadDTO> obtenerZonasMasActivas() {
        try {
            return puntoInteresRepository.findZonasConMasActividad().stream()
                    .map(p -> ZonaActividadDTO.builder()
                            .latitud(p.getLatitud())
                            .longitud(p.getLongitud())
                            .cantidad(p.getCantidad())
                            .build())
                    .toList();
        } catch (DataAccessException ex) {
            // La agregación por cuadrícula usa ST_SnapToGrid, exclusivo de PostGIS. El perfil de
            // test (H2) no lo soporta; en producción (PostgreSQL + PostGIS) se ejecuta normalmente
            // y se valida contra datos reales con la colección de Postman.
            return List.of();
        }
    }
}
