package _0.motovias_backend.service;

import _0.motovias_backend.dto.AporteMensualDTO;
import _0.motovias_backend.dto.DashboardResponseDTO;
import _0.motovias_backend.dto.UserProfileResponseDTO;
import _0.motovias_backend.dto.UserProfileUpdateDTO;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.TipoVoto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.repository.GastoRepository;
import _0.motovias_backend.repository.NotificacionRepository;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteEventoRepository;
import _0.motovias_backend.repository.ReporteVotoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.repository.ViajeParticipanteRepository;
import _0.motovias_backend.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionRepository notificacionRepository;
    private final ReporteVotoRepository reporteVotoRepository;
    private final ReporteEventoRepository reporteEventoRepository;
    private final PuntoInteresRepository puntoInteresRepository;
    private final ViajeParticipanteRepository viajeParticipanteRepository;
    private final GastoRepository gastoRepository;
    private final ViajeRepository viajeRepository;

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int MESES_HISTORIAL_DASHBOARD = 6;

    public UserProfileResponseDTO getProfile(String email) {
        User user = findByEmailOrThrow(email);
        return toResponseDTO(user);
    }

    public UserProfileResponseDTO updateProfile(String email, UserProfileUpdateDTO dto) {
        User user = findByEmailOrThrow(email);
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setTipoMotocicleta(dto.getTipoMotocicleta());
        user.setTipoSangre(dto.getTipoSangre());
        user.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        user.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        user.setDireccion(dto.getDireccion());
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        userRepository.save(user);
        return toResponseDTO(user);
    }

    /**
     * Elimina en cascada al usuario autenticado y todos los registros que dependen
     * de él. El orden respeta las restricciones de clave foránea de las tablas que
     * no tienen ON DELETE CASCADE configurado a nivel de base de datos.
     */
    @Transactional
    public void eliminarCuenta(String email) {
        User user = findByEmailOrThrow(email);

        notificacionRepository.deleteByDestinatario(user);
        reporteVotoRepository.deleteByUsuario(user);
        reporteEventoRepository.deleteByUsuario(user);

        List<PuntoInteres> reportesPropios =
                puntoInteresRepository.findByUsuarioIdOrderByFechaCreacionDesc(user.getId());
        for (PuntoInteres reporte : reportesPropios) {
            reporteVotoRepository.deleteByReporte(reporte);
        }
        puntoInteresRepository.deleteAll(reportesPropios);

        viajeParticipanteRepository.deleteByUsuario(user);
        gastoRepository.deleteByPagador(user);

        List<Viaje> viajesPropios = viajeRepository.findByCreador(user);
        for (Viaje viaje : viajesPropios) {
            gastoRepository.deleteByViaje(viaje);
            viajeParticipanteRepository.deleteByViaje(viaje);
        }
        viajeRepository.deleteAll(viajesPropios);

        userRepository.delete(user);
    }

    public DashboardResponseDTO obtenerMiDashboard(String email) {
        User usuario = findByEmailOrThrow(email);

        long reportesActivos = puntoInteresRepository.countByUsuarioIdAndEstado(usuario.getId(), EstadoPunto.ACTIVO);
        long confirmaciones = reporteVotoRepository.countByReporteUsuarioAndTipoVoto(usuario, TipoVoto.CONFIRMA);
        long refutaciones = reporteVotoRepository.countByReporteUsuarioAndTipoVoto(usuario, TipoVoto.REFUTA);
        long caravanasParticipando = viajeParticipanteRepository.countByUsuario(usuario);

        LocalDate inicioSemestre = ZonedDateTime.now(ZONA_ARGENTINA).toLocalDate()
                .withDayOfMonth(1)
                .minusMonths(MESES_HISTORIAL_DASHBOARD - 1L);

        List<AporteMensualDTO> aportesPorMes = puntoInteresRepository
                .countMensualPorUsuario(usuario, inicioSemestre.atStartOfDay())
                .stream()
                .map(p -> AporteMensualDTO.builder()
                        .anio(p.getAnio())
                        .mes(p.getMes())
                        .cantidad(p.getCantidad())
                        .build())
                .toList();

        return DashboardResponseDTO.builder()
                .reportesActivos(reportesActivos)
                .votosRecibidos(confirmaciones - refutaciones)
                .caravanasParticipando(caravanasParticipando)
                .aportesPorMes(aportesPorMes)
                .build();
    }

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private UserProfileResponseDTO toResponseDTO(User user) {
        return new UserProfileResponseDTO(
                user.getNombre(),
                user.getApellido(),
                user.getEmail(),
                user.getTipoMotocicleta(),
                user.isActivo(),
                user.getRole().name(),
                user.getTipoSangre(),
                user.getContactoEmergenciaNombre(),
                user.getContactoEmergenciaTelefono(),
                user.getDireccion()
        );
    }
}
