package _0.motovias_backend.service;

import _0.motovias_backend.dto.NotificacionResponseDTO;
import _0.motovias_backend.model.*;
import _0.motovias_backend.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificacionService notificacionService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private User usuario;
    private PuntoInteres reporte;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(1L);
        usuario.setEmail("rider@motovias.com");
        usuario.setNombre("Juan");
        usuario.setRole(Role.USER);

        reporte = PuntoInteres.builder()
                .id(10L)
                .titulo("SOS en ruta")
                .categoria(Categoria.ALERTA_SOS)
                .estado(EstadoPunto.ACTIVO)
                .ubicacion(GF.createPoint(new Coordinate(-64.1811, -31.4135)))
                .fechaCreacion(LocalDateTime.now().minusHours(3))
                .build();
        reporte.setUsuario(usuario);
    }

    // ── crearYEmitirSiNoExiste ──────────────────────────────────────────────────

    @Test
    @DisplayName("crea notificación y emite por WebSocket cuando no existe una previa")
    void crearYEmitir_cuandoNoExisteDuplicado_persisteYEmite() {
        when(notificacionRepository.existsByReporteAndDestinatarioAndTipoAndLeidaFalse(
                reporte, usuario, TipoNotificacion.RECORDATORIO_CIERRE)).thenReturn(false);

        Notificacion guardada = buildNotificacion(1L, reporte, usuario);
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(guardada);

        notificacionService.crearYEmitirSiNoExiste(reporte);

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/usuarios/rider@motovias.com/notificaciones"),
                any(NotificacionResponseDTO.class));
    }

    @Test
    @DisplayName("no crea notificación si ya existe una no leída para el mismo reporte")
    void crearYEmitir_cuandoYaExisteDuplicado_noHaceNada() {
        when(notificacionRepository.existsByReporteAndDestinatarioAndTipoAndLeidaFalse(
                reporte, usuario, TipoNotificacion.RECORDATORIO_CIERRE)).thenReturn(true);

        notificacionService.crearYEmitirSiNoExiste(reporte);

        verify(notificacionRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("no crea notificación si el reporte no tiene usuario")
    void crearYEmitir_reporteSinUsuario_noHaceNada() {
        reporte.setUsuario(null);

        notificacionService.crearYEmitirSiNoExiste(reporte);

        verify(notificacionRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("el mensaje generado menciona el título del reporte")
    void crearYEmitir_mensajeContieneNombreDelReporte() {
        when(notificacionRepository.existsByReporteAndDestinatarioAndTipoAndLeidaFalse(
                any(), any(), any())).thenReturn(false);

        Notificacion guardada = buildNotificacion(1L, reporte, usuario);
        when(notificacionRepository.save(any())).thenReturn(guardada);

        notificacionService.crearYEmitirSiNoExiste(reporte);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getMensaje()).contains("SOS en ruta");
    }

    // ── archivarPorReporte ─────────────────────────────────────────────────────

    @Test
    @DisplayName("archivar marca todas las notificaciones pendientes del reporte como leídas")
    void archivar_marcaTodasPendientesComoLeidas() {
        Notificacion n1 = buildNotificacion(1L, reporte, usuario);
        Notificacion n2 = buildNotificacion(2L, reporte, usuario);
        when(notificacionRepository.findByReporteAndLeidaFalse(reporte)).thenReturn(List.of(n1, n2));

        notificacionService.archivarPorReporte(reporte);

        assertThat(n1.isLeida()).isTrue();
        assertThat(n2.isLeida()).isTrue();
        verify(notificacionRepository).saveAll(List.of(n1, n2));
    }

    @Test
    @DisplayName("archivar sin notificaciones pendientes no falla")
    void archivar_sinPendientes_noFalla() {
        when(notificacionRepository.findByReporteAndLeidaFalse(reporte)).thenReturn(Collections.emptyList());

        notificacionService.archivarPorReporte(reporte);

        verify(notificacionRepository).saveAll(Collections.emptyList());
    }

    // ── marcarLeida ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("marcarLeida lanza 404 si la notificación no pertenece al usuario")
    void marcarLeida_notificacionNoEncontrada_lanza404() {
        when(notificacionRepository.findByIdAndDestinatario(99L, usuario)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(99L, usuario))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("marcarLeida actualiza el campo leida a true y retorna el DTO")
    void marcarLeida_existente_actualizaYRetornaDTO() {
        Notificacion notificacion = buildNotificacion(1L, reporte, usuario);
        when(notificacionRepository.findByIdAndDestinatario(1L, usuario)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(notificacion)).thenReturn(notificacion);

        NotificacionResponseDTO resultado = notificacionService.marcarLeida(1L, usuario);

        assertThat(notificacion.isLeida()).isTrue();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.isLeida()).isTrue();
    }

    // ── listarNoLeidas ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarNoLeidas retorna solo las notificaciones no leídas del usuario")
    void listarNoLeidas_retornaListaCorrecta() {
        Notificacion n = buildNotificacion(1L, reporte, usuario);
        when(notificacionRepository.findByDestinatarioAndLeidaFalseOrderByFechaCreacionDesc(usuario))
                .thenReturn(List.of(n));

        List<NotificacionResponseDTO> resultado = notificacionService.listarNoLeidas(usuario);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
        assertThat(resultado.get(0).getReporteId()).isEqualTo(10L);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Notificacion buildNotificacion(Long id, PuntoInteres r, User dest) {
        return Notificacion.builder()
                .id(id)
                .mensaje("Tu alerta SOS \"" + r.getTitulo() + "\" lleva más de 2 horas activa. ¿Ya fue atendida?")
                .tipo(TipoNotificacion.RECORDATORIO_CIERRE)
                .fechaCreacion(LocalDateTime.now())
                .leida(false)
                .destinatario(dest)
                .reporte(r)
                .build();
    }
}
