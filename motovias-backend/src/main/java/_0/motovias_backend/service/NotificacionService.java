package _0.motovias_backend.service;

import _0.motovias_backend.dto.NotificacionResponseDTO;
import _0.motovias_backend.model.Notificacion;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.TipoNotificacion;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<NotificacionResponseDTO> listarNoLeidas(User destinatario) {
        return notificacionRepository
                .findByDestinatarioAndLeidaFalseOrderByFechaCreacionDesc(destinatario)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public NotificacionResponseDTO marcarLeida(Long id, User destinatario) {
        Notificacion notificacion = notificacionRepository.findByIdAndDestinatario(id, destinatario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));

        notificacion.setLeida(true);
        return toDTO(notificacionRepository.save(notificacion));
    }

    @Transactional
    public void archivarPorReporte(PuntoInteres reporte) {
        List<Notificacion> pendientes = notificacionRepository.findByReporteAndLeidaFalse(reporte);
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    @Transactional
    public void crearYEmitirSiNoExiste(PuntoInteres reporte) {
        User destinatario = reporte.getUsuario();
        if (destinatario == null) return;

        boolean yaExiste = notificacionRepository.existsByReporteAndDestinatarioAndTipoAndLeidaFalse(
                reporte, destinatario, TipoNotificacion.RECORDATORIO_CIERRE);

        if (yaExiste) return;

        Notificacion nueva = Notificacion.builder()
                .mensaje("Tu alerta SOS \"" + reporte.getTitulo() + "\" lleva más de 2 horas activa. ¿Ya fue atendida?")
                .tipo(TipoNotificacion.RECORDATORIO_CIERRE)
                .destinatario(destinatario)
                .reporte(reporte)
                .build();

        Notificacion guardada = notificacionRepository.save(nueva);
        messagingTemplate.convertAndSend(
                "/topic/usuarios/" + destinatario.getEmail() + "/notificaciones",
                toDTO(guardada));
    }

    private NotificacionResponseDTO toDTO(Notificacion n) {
        return NotificacionResponseDTO.builder()
                .id(n.getId())
                .mensaje(n.getMensaje())
                .fechaCreacion(n.getFechaCreacion() != null ? n.getFechaCreacion().format(ISO_FMT) : null)
                .leida(n.isLeida())
                .tipo(n.getTipo())
                .reporteId(n.getReporte().getId())
                .reporteTitulo(n.getReporte().getTitulo())
                .build();
    }
}
