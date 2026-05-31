package _0.motovias_backend.service;

import _0.motovias_backend.dto.PuntoInteresRequestDTO;
import _0.motovias_backend.dto.PuntoInteresResponseDTO;
import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuntoInteresServiceTest {

    @Mock
    private PuntoInteresRepository repository;

    @InjectMocks
    private PuntoInteresService service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    // Coordenadas de referencia: centro de Córdoba Capital
    private static final double LAT = -31.4135;
    private static final double LON = -64.1811;

    @Test
    @DisplayName("listarTodos retorna lista vacía cuando no hay puntos")
    void listarTodos_sinDatos_retornaListaVacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listarTodos()).isEmpty();
    }

    @Test
    @DisplayName("listarTodos mapea coordenadas: getY()=lat, getX()=lon")
    void listarTodos_mapeaCoordenadas_correctamente() {
        PuntoInteres entidad = puntoConCoordenadas(1L, LON, LAT, Categoria.ACCIDENTE);
        when(repository.findAll()).thenReturn(List.of(entidad));

        List<PuntoInteresResponseDTO> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        PuntoInteresResponseDTO dto = resultado.get(0);
        assertThat(dto.getLatitud()).isCloseTo(LAT, Offset.offset(0.0001));
        assertThat(dto.getLongitud()).isCloseTo(LON, Offset.offset(0.0001));
        assertThat(dto.getCategoria()).isEqualTo(Categoria.ACCIDENTE);
        assertThat(dto.getEmailUsuario()).isNull();
    }

    @Test
    @DisplayName("listarTodos incluye email del usuario cuando existe")
    void listarTodos_conUsuario_incluyeEmail() {
        PuntoInteres entidad = puntoConCoordenadas(2L, LON, LAT, Categoria.OBRA);
        User user = new User();
        user.setEmail("test@motovias.com");
        entidad.setUsuario(user);
        when(repository.findAll()).thenReturn(List.of(entidad));

        PuntoInteresResponseDTO dto = service.listarTodos().get(0);

        assertThat(dto.getEmailUsuario()).isEqualTo("test@motovias.com");
    }

    @Test
    @DisplayName("buscarCercanos delega en el repositorio con los parámetros correctos")
    void buscarCercanos_delegaEnRepositorio() {
        double radio = 500.0;
        when(repository.findCercanos(LAT, LON, radio)).thenReturn(List.of());

        List<PuntoInteresResponseDTO> resultado = service.buscarCercanos(LAT, LON, radio);

        assertThat(resultado).isEmpty();
        verify(repository).findCercanos(LAT, LON, radio);
    }

    @Test
    @DisplayName("buscarCercanos mapea la respuesta del repositorio a DTOs")
    void buscarCercanos_mapeaRespuestaADTOs() {
        PuntoInteres entidad = puntoConCoordenadas(3L, LON, LAT, Categoria.TRAMPA_POLICIAL);
        when(repository.findCercanos(LAT, LON, 1000.0)).thenReturn(List.of(entidad));

        List<PuntoInteresResponseDTO> resultado = service.buscarCercanos(LAT, LON, 1000.0);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCategoria()).isEqualTo(Categoria.TRAMPA_POLICIAL);
    }

    @Test
    @DisplayName("crear persiste el punto y retorna el DTO con id asignado")
    void crear_persisteYRetornaDTO() {
        PuntoInteresRequestDTO dto = new PuntoInteresRequestDTO();
        dto.setTitulo("Bache peligroso");
        dto.setDescripcion("Gran bache en cruce");
        dto.setLatitud(LAT);
        dto.setLongitud(LON);
        dto.setCategoria(Categoria.PELIGRO);

        PuntoInteres guardado = puntoConCoordenadas(10L, LON, LAT, Categoria.PELIGRO);
        guardado.setTitulo("Bache peligroso");
        guardado.setDescripcion("Gran bache en cruce");
        when(repository.save(any(PuntoInteres.class))).thenReturn(guardado);

        PuntoInteresResponseDTO resultado = service.crear(dto, null);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getTitulo()).isEqualTo("Bache peligroso");
        assertThat(resultado.getCategoria()).isEqualTo(Categoria.PELIGRO);
        assertThat(resultado.getLatitud()).isCloseTo(LAT, Offset.offset(0.0001));
        verify(repository).save(any(PuntoInteres.class));
    }

    @Test
    @DisplayName("crear construye Point JTS con coordenadas en orden (x=lon, y=lat)")
    void crear_construyePointConOrdenCorrecto() {
        PuntoInteresRequestDTO dto = new PuntoInteresRequestDTO();
        dto.setTitulo("Test");
        dto.setLatitud(LAT);
        dto.setLongitud(LON);
        dto.setCategoria(Categoria.SEMAFORO_ROTO);

        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> {
            PuntoInteres p = inv.getArgument(0);
            // Verificar que X=longitud, Y=latitud dentro del save
            assertThat(p.getUbicacion().getX()).isCloseTo(LON, Offset.offset(0.0001));
            assertThat(p.getUbicacion().getY()).isCloseTo(LAT, Offset.offset(0.0001));
            p.setId(1L);
            return p;
        });

        service.crear(dto, null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private PuntoInteres puntoConCoordenadas(Long id, double x, double y, Categoria cat) {
        return PuntoInteres.builder()
                .id(id)
                .titulo("Punto " + id)
                .descripcion("Descripcion " + id)
                .categoria(cat)
                .ubicacion(GF.createPoint(new Coordinate(x, y)))
                .build();
    }
}
