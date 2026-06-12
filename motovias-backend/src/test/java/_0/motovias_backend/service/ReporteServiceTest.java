package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import org.assertj.core.data.Offset;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PuntoInteresRepository repository;

    @InjectMocks
    private ReporteService service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double LAT = -31.4135;
    private static final double LON = -64.1811;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setEmail("rider@motovias.com");
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
    }

    @Test
    @DisplayName("crear persiste la entidad y retorna DTO con todos los campos mapeados")
    void crear_persisteYRetornaDTO() {
        PuntoInteres guardado = puntoGuardado(10L, Categoria.GOMERIA, EstadoPunto.ACTIVO);
        when(repository.save(any(PuntoInteres.class))).thenReturn(guardado);

        ReporteRequestDTO dto = reporteDTO("Gomería Norte", "Atiende 24hs", Categoria.GOMERIA, LAT, LON);

        ReporteResponseDTO resultado = service.crear(dto, usuario);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getTitulo()).isEqualTo("Gomería Norte");
        assertThat(resultado.getDescripcion()).isEqualTo("Atiende 24hs");
        assertThat(resultado.getCategoria()).isEqualTo(Categoria.GOMERIA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.ACTIVO);
        assertThat(resultado.getLatitud()).isCloseTo(LAT, Offset.offset(0.0001));
        assertThat(resultado.getLongitud()).isCloseTo(LON, Offset.offset(0.0001));
        assertThat(resultado.getEmailUsuario()).isEqualTo("rider@motovias.com");
        assertThat(resultado.getNombreUsuario()).isEqualTo("Juan Perez");
        assertThat(resultado.getFechaCreacion()).isNotNull();

        verify(repository).save(any(PuntoInteres.class));
    }

    @Test
    @DisplayName("crear construye Point JTS con orden correcto: x=longitud, y=latitud")
    void crear_construyePointConOrdenCorrecto() {
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> {
            PuntoInteres p = inv.getArgument(0);
            assertThat(p.getUbicacion().getX()).isCloseTo(LON, Offset.offset(0.0001));
            assertThat(p.getUbicacion().getY()).isCloseTo(LAT, Offset.offset(0.0001));
            p.setId(1L);
            return p;
        });

        service.crear(reporteDTO("Test", null, Categoria.TALLER, LAT, LON), usuario);
    }

    @Test
    @DisplayName("crear con categoría ALERTA_SOS guarda correctamente")
    void crear_categoriaAlertaSos() {
        PuntoInteres guardado = puntoGuardado(20L, Categoria.ALERTA_SOS, EstadoPunto.ACTIVO);
        when(repository.save(any(PuntoInteres.class))).thenReturn(guardado);

        ReporteResponseDTO resultado = service.crear(
                reporteDTO("Caída en ruta", "Necesito ayuda", Categoria.ALERTA_SOS, LAT, LON),
                usuario
        );

        assertThat(resultado.getCategoria()).isEqualTo(Categoria.ALERTA_SOS);
    }

    @Test
    @DisplayName("toDTO mapea usuario nulo sin lanzar excepción")
    void toDTO_usuarioNulo_noLanzaExcepcion() {
        PuntoInteres sinUsuario = PuntoInteres.builder()
                .id(5L)
                .titulo("Sin dueño")
                .categoria(Categoria.PUNTO_INTERES)
                .ubicacion(GF.createPoint(new Coordinate(LON, LAT)))
                .build();

        ReporteResponseDTO dto = service.toDTO(sinUsuario);

        assertThat(dto.getEmailUsuario()).isNull();
        assertThat(dto.getNombreUsuario()).isNull();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private ReporteRequestDTO reporteDTO(String titulo, String desc, Categoria cat, double lat, double lon) {
        ReporteRequestDTO dto = new ReporteRequestDTO();
        dto.setTitulo(titulo);
        dto.setDescripcion(desc);
        dto.setCategoria(cat);
        dto.setLatitud(lat);
        dto.setLongitud(lon);
        return dto;
    }

    private PuntoInteres puntoGuardado(Long id, Categoria cat, EstadoPunto estado) {
        PuntoInteres p = PuntoInteres.builder()
                .id(id)
                .titulo("Gomería Norte")
                .descripcion("Atiende 24hs")
                .categoria(cat)
                .estado(estado)
                .ubicacion(GF.createPoint(new Coordinate(LON, LAT)))
                .fechaCreacion(LocalDateTime.of(2026, 6, 11, 12, 0))
                .build();
        p.setUsuario(usuario);
        return p;
    }
}
