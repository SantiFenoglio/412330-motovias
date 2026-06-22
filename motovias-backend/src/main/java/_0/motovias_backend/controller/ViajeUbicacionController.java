package _0.motovias_backend.controller;

import _0.motovias_backend.dto.ParticipanteUbicacionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class ViajeUbicacionController {

    private final SimpMessagingTemplate messagingTemplate;

    // sessionId STOMP -> último payload conocido (incluye viajeId para saber a qué topic notificar)
    private final ConcurrentHashMap<String, ParticipanteUbicacionDTO> sesionesActivas =
            new ConcurrentHashMap<>();

    @MessageMapping("/viajes/{viajeId}/compartir")
    public void compartirUbicacion(
            @DestinationVariable Long viajeId,
            ParticipanteUbicacionDTO dto,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        dto.setViajeId(viajeId);
        dto.setConectado(true);

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            sesionesActivas.put(sessionId, dto);
        }

        messagingTemplate.convertAndSend("/topic/viajes/" + viajeId + "/ubicaciones", dto);
    }

    @EventListener
    public void manejarDesconexion(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        ParticipanteUbicacionDTO ultimaUbicacion = sesionesActivas.remove(sessionId);

        if (ultimaUbicacion == null) {
            return;
        }

        // Notifica al resto del grupo que este participante se desconectó
        ParticipanteUbicacionDTO desconectado = ParticipanteUbicacionDTO.builder()
                .viajeId(ultimaUbicacion.getViajeId())
                .email(ultimaUbicacion.getEmail())
                .nombre(ultimaUbicacion.getNombre())
                .latitud(null)
                .longitud(null)
                .conectado(false)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/viajes/" + ultimaUbicacion.getViajeId() + "/ubicaciones",
                desconectado
        );
    }
}
