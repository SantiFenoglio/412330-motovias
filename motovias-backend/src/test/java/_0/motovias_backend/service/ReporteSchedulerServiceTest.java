package _0.motovias_backend.service;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.repository.PuntoInteresRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteSchedulerServiceTest {

    @Mock
    private PuntoInteresRepository puntoInteresRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ReporteSchedulerService schedulerService;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("alertas SOS vencidas → crearYEmitirSiNoExiste llamado para cada una")
    void verificar_conAlertasVencidas_llama_crearNotificacionParaCadaUna() {
        PuntoInteres alerta1 = buildAlerta(1L, LocalDateTime.now().minusHours(3));
        PuntoInteres alerta2 = buildAlerta(2L, LocalDateTime.now().minusHours(5));

        when(puntoInteresRepository.findAlertasSosVencidas(
                eq(Categoria.ALERTA_SOS), eq(EstadoPunto.ACTIVO), any(LocalDateTime.class)))
                .thenReturn(List.of(alerta1, alerta2));

        schedulerService.verificarAlertasSosVencidas();

        verify(notificacionService).crearYEmitirSiNoExiste(alerta1);
        verify(notificacionService).crearYEmitirSiNoExiste(alerta2);
    }

    @Test
    @DisplayName("sin alertas vencidas → servicio de notificaciones no es llamado")
    void verificar_sinAlertasVencidas_noLlamaServicio() {
        when(puntoInteresRepository.findAlertasSosVencidas(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        schedulerService.verificarAlertasSosVencidas();

        verify(notificacionService, never()).crearYEmitirSiNoExiste(any());
    }

    @Test
    @DisplayName("el límite temporal pasado al repositorio es ahora menos HORAS_LIMITE")
    void verificar_limiteTemporal_esCorrecto() {
        when(puntoInteresRepository.findAlertasSosVencidas(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDateTime antes = LocalDateTime.now().minusHours(ReporteSchedulerService.HORAS_LIMITE);
        schedulerService.verificarAlertasSosVencidas();
        LocalDateTime despues = LocalDateTime.now().minusHours(ReporteSchedulerService.HORAS_LIMITE);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(puntoInteresRepository).findAlertasSosVencidas(
                eq(Categoria.ALERTA_SOS), eq(EstadoPunto.ACTIVO), captor.capture());

        LocalDateTime limiteCapturado = captor.getValue();
        assertThat(limiteCapturado).isBetween(antes, despues);
    }

    @Test
    @DisplayName("solo busca reportes ALERTA_SOS en estado ACTIVO")
    void verificar_filtroCorrectoDeCategoriaYEstado() {
        when(puntoInteresRepository.findAlertasSosVencidas(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        schedulerService.verificarAlertasSosVencidas();

        verify(puntoInteresRepository).findAlertasSosVencidas(
                eq(Categoria.ALERTA_SOS), eq(EstadoPunto.ACTIVO), any());
    }

    private PuntoInteres buildAlerta(Long id, LocalDateTime fechaCreacion) {
        return PuntoInteres.builder()
                .id(id)
                .titulo("SOS en ruta")
                .categoria(Categoria.ALERTA_SOS)
                .estado(EstadoPunto.ACTIVO)
                .ubicacion(GF.createPoint(new Coordinate(-64.1811, -31.4135)))
                .fechaCreacion(fechaCreacion)
                .build();
    }
}
