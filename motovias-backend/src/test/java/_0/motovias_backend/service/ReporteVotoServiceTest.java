package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.dto.VotoRequestDTO;
import _0.motovias_backend.model.*;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteVotoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteVotoServiceTest {

    @Mock
    private PuntoInteresRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReporteVotoRepository votoRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ReporteService service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double LAT = -31.4135;
    private static final double LON = -64.1811;

    private User creador;
    private User votante;
    private PuntoInteres punto;

    @BeforeEach
    void setUp() {
        creador = new User();
        creador.setId(1L);
        creador.setEmail("creador@motovias.com");
        creador.setNombre("Ana");
        creador.setApellido("Lopez");
        creador.setRole(Role.USER);
        creador.setActivo(true);

        votante = new User();
        votante.setId(2L);
        votante.setEmail("votante@motovias.com");
        votante.setNombre("Carlos");
        votante.setApellido("Gomez");
        votante.setRole(Role.USER);
        votante.setActivo(true);

        punto = PuntoInteres.builder()
                .id(10L)
                .titulo("Bache peligroso")
                .descripcion("Hay un bache grande en la esquina")
                .categoria(Categoria.ALERTA_SOS)
                .estado(EstadoPunto.ACTIVO)
                .ubicacion(GF.createPoint(new Coordinate(LON, LAT)))
                .fechaCreacion(LocalDateTime.of(2026, 6, 15, 10, 0))
                .build();
        punto.setUsuario(creador);
    }

    @Test
    @DisplayName("votar registra el voto y retorna DTO con conteos correctos")
    void votar_registraVotoExitosamente() {
        VotoRequestDTO dto = new VotoRequestDTO();
        dto.setTipoVoto(TipoVoto.CONFIRMA);

        when(repository.findById(10L)).thenReturn(Optional.of(punto));
        when(votoRepository.findByUsuarioAndReporte(votante, punto)).thenReturn(Optional.empty());
        when(votoRepository.save(any(ReporteVoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA)).thenReturn(1L);
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA)).thenReturn(0L);

        ReporteResponseDTO resultado = service.votar(10L, dto, votante);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getConfirmaciones()).isEqualTo(1L);
        assertThat(resultado.getRefutaciones()).isEqualTo(0L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.ACTIVO);
        verify(votoRepository).save(argThat(v -> v.getTipoVoto() == TipoVoto.CONFIRMA));
    }

    @Test
    @DisplayName("votar lanza 400 cuando el usuario intenta votar su propio reporte")
    void votar_bloqueaAutovoto() {
        VotoRequestDTO dto = new VotoRequestDTO();
        dto.setTipoVoto(TipoVoto.CONFIRMA);

        when(repository.findById(10L)).thenReturn(Optional.of(punto));

        assertThatThrownBy(() -> service.votar(10L, dto, creador))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException ex = (ResponseStatusException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("No podés votar tu propio reporte");
                });

        verify(votoRepository, never()).save(any());
        verify(votoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("votar muta el estado a DUDOSO cuando el balance de votos llega a -5")
    void votar_mutaADudosoCuandoBalanceEsIgualAMenos5() {
        VotoRequestDTO dto = new VotoRequestDTO();
        dto.setTipoVoto(TipoVoto.REFUTA);

        when(repository.findById(10L)).thenReturn(Optional.of(punto));
        when(votoRepository.findByUsuarioAndReporte(votante, punto)).thenReturn(Optional.empty());
        when(votoRepository.save(any(ReporteVoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA)).thenReturn(0L);
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA)).thenReturn(5L);
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteResponseDTO resultado = service.votar(10L, dto, votante);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.DUDOSO);
        assertThat(resultado.getRefutaciones()).isEqualTo(5L);
        verify(repository).save(argThat(p -> p.getEstado() == EstadoPunto.DUDOSO));
    }

    @Test
    @DisplayName("votar cancela el voto si el usuario ya votó con el mismo tipo")
    void votar_cancelaVotoSiYaVotoConMismoTipo() {
        VotoRequestDTO dto = new VotoRequestDTO();
        dto.setTipoVoto(TipoVoto.CONFIRMA);

        ReporteVoto votoExistente = ReporteVoto.builder()
                .id(99L)
                .usuario(votante)
                .reporte(punto)
                .tipoVoto(TipoVoto.CONFIRMA)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(punto));
        when(votoRepository.findByUsuarioAndReporte(votante, punto)).thenReturn(Optional.of(votoExistente));
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA)).thenReturn(0L);
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA)).thenReturn(0L);

        service.votar(10L, dto, votante);

        verify(votoRepository).delete(votoExistente);
        verify(votoRepository, never()).save(any());
    }

    @Test
    @DisplayName("votar cambia el tipo si el usuario ya votó con el tipo opuesto")
    void votar_cambiaVotoSiTipoEsOpuesto() {
        VotoRequestDTO dto = new VotoRequestDTO();
        dto.setTipoVoto(TipoVoto.REFUTA);

        ReporteVoto votoExistente = ReporteVoto.builder()
                .id(99L)
                .usuario(votante)
                .reporte(punto)
                .tipoVoto(TipoVoto.CONFIRMA)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(punto));
        when(votoRepository.findByUsuarioAndReporte(votante, punto)).thenReturn(Optional.of(votoExistente));
        when(votoRepository.save(any(ReporteVoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA)).thenReturn(0L);
        when(votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA)).thenReturn(1L);

        service.votar(10L, dto, votante);

        verify(votoRepository).save(argThat(v -> v.getTipoVoto() == TipoVoto.REFUTA));
        verify(votoRepository, never()).delete(any());
    }
}
