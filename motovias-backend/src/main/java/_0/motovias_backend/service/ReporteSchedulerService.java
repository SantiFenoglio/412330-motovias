package _0.motovias_backend.service;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.repository.PuntoInteresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteSchedulerService {

    private final PuntoInteresRepository puntoInteresRepository;
    private final NotificacionService notificacionService;

    static final long HORAS_LIMITE = 2;

    @Scheduled(cron = "0 0 * * * *")
    public void verificarAlertasSosVencidas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(HORAS_LIMITE);
        List<PuntoInteres> vencidas = puntoInteresRepository.findAlertasSosVencidas(
                Categoria.ALERTA_SOS, EstadoPunto.ACTIVO, limite);

        vencidas.forEach(notificacionService::crearYEmitirSiNoExiste);
    }
}
