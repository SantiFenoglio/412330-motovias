package _0.motovias_backend.service;

import _0.motovias_backend.dto.UserProfileResponseDTO;
import _0.motovias_backend.dto.UserProfileUpdateDTO;
import _0.motovias_backend.model.PuntoInteres;
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
