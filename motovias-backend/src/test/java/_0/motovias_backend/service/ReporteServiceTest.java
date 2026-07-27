package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteEventoResponseDTO;
import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.dto.ReporteUpdateDTO;
import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.FuenteUbicacion;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteEvento;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.TipoEventoReporte;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.FotoReporteRepository;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteEventoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.service.NotificacionService;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PuntoInteresRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReporteEventoRepository eventoRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private FotoReporteRepository fotoReporteRepository;

    @InjectMocks
    private ReporteService service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double LAT = -31.4135;
    private static final double LON = -64.1811;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(1L);
        usuario.setEmail("rider@motovias.com");
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setRole(Role.USER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Tests existentes ────────────────────────────────────────────────────────

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
        assertThat(resultado.getFuenteUbicacion()).isEqualTo(FuenteUbicacion.GPS);

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

    // ── Tests de fuenteUbicacion (US-27) ────────────────────────────────────

    @Test
    @DisplayName("crear persiste fuenteUbicacion MANUAL cuando se envía en el DTO")
    void crear_persisteFuenteUbicacionManual() {
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> {
            PuntoInteres p = inv.getArgument(0);
            assertThat(p.getFuenteUbicacion()).isEqualTo(FuenteUbicacion.MANUAL);
            p.setId(99L);
            return p;
        });

        ReporteRequestDTO dto = reporteDTO("Ubicación manual", null, Categoria.PUNTO_INTERES, LAT, LON);
        dto.setFuenteUbicacion(FuenteUbicacion.MANUAL);

        ReporteResponseDTO resultado = service.crear(dto, usuario);

        assertThat(resultado.getFuenteUbicacion()).isEqualTo(FuenteUbicacion.MANUAL);
    }

    @Test
    @DisplayName("crear usa GPS como fuente por defecto cuando fuenteUbicacion no se envía")
    void crear_usaGpsPorDefectoCuandoFuenteEsNula() {
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> {
            PuntoInteres p = inv.getArgument(0);
            assertThat(p.getFuenteUbicacion()).isEqualTo(FuenteUbicacion.GPS);
            p.setId(100L);
            return p;
        });

        ReporteRequestDTO dto = reporteDTO("Sin fuente", null, Categoria.GOMERIA, LAT, LON);

        ReporteResponseDTO resultado = service.crear(dto, usuario);

        assertThat(resultado.getFuenteUbicacion()).isEqualTo(FuenteUbicacion.GPS);
    }

    // ── Tests de listarMios ──────────────────────────────────────────────────

    @Test
    @DisplayName("listarMios consulta el repositorio excluyendo los reportes dados de baja lógica")
    void listarMios_excluyeReportesEliminados() {
        PuntoInteres activo = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);
        when(repository.findByUsuarioIdAndEstadoNotOrderByFechaCreacionDesc(usuario.getId(), EstadoPunto.ELIMINADO))
                .thenReturn(List.of(activo));

        List<ReporteResponseDTO> resultado = service.listarMios(usuario);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoPunto.ACTIVO);
        verify(repository).findByUsuarioIdAndEstadoNotOrderByFechaCreacionDesc(usuario.getId(), EstadoPunto.ELIMINADO);
        verify(repository, never()).findByUsuarioIdOrderByFechaCreacionDesc(any());
    }

    // ── Tests de seguridad (US-25) ───────────────────────────────────────────

    @Test
    @DisplayName("editar lanza 403 cuando el usuario autenticado no es dueño del reporte ni ADMIN")
    void editar_usuarioNoAutorizado_lanza403() {
        setAuthentication("intruso@motovias.com");

        User intruso = crearUsuario(2L, "intruso@motovias.com", Role.USER);
        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("intruso@motovias.com")).thenReturn(Optional.of(intruso));

        ReporteUpdateDTO dto = new ReporteUpdateDTO();
        dto.setEstado(EstadoPunto.RESUELTO);

        assertThatThrownBy(() -> service.editar(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar lanza 403 cuando el usuario autenticado no es dueño del reporte ni ADMIN")
    void eliminar_usuarioNoAutorizado_lanza403() {
        setAuthentication("intruso@motovias.com");

        User intruso = crearUsuario(2L, "intruso@motovias.com", Role.USER);
        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("intruso@motovias.com")).thenReturn(Optional.of(intruso));

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(repository, never()).save(any(PuntoInteres.class));
    }

    @Test
    @DisplayName("eliminar realiza baja lógica: setea ELIMINADO, guarda y audita el evento")
    void eliminar_propietarioAutorizado_marcaEstadoEliminadoYAudita() {
        setAuthentication("rider@motovias.com");

        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);
        punto.setUsuario(usuario);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("rider@motovias.com")).thenReturn(Optional.of(usuario));
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteResponseDTO resultado = service.eliminar(1L);

        // La entidad nunca se borra físicamente: se conserva vía save() con estado ELIMINADO
        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.ELIMINADO);
        verify(repository, never()).delete(any(PuntoInteres.class));
        verify(repository, never()).deleteById(any());
        verify(repository).save(argThat(p -> p.getEstado() == EstadoPunto.ELIMINADO));

        // Queda auditado en el historial del reporte
        verify(eventoRepository).save(argThat(evento ->
                evento.getTipoEvento() == TipoEventoReporte.CAMBIO_ESTADO
                        && evento.getDescripcion().contains("ELIMINADO")));
    }

    @Test
    @DisplayName("editar permite al ADMIN modificar el reporte de otro usuario")
    void editar_adminPuedeModificarReporteAjeno() {
        setAuthentication("admin@motovias.com");

        User admin = crearUsuario(99L, "admin@motovias.com", Role.ADMIN);
        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("admin@motovias.com")).thenReturn(Optional.of(admin));
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteUpdateDTO dto = new ReporteUpdateDTO();
        dto.setDescripcion("Corregido por admin");
        dto.setEstado(EstadoPunto.RESUELTO);

        ReporteResponseDTO resultado = service.editar(1L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.RESUELTO);
        assertThat(resultado.getDescripcion()).isEqualTo("Corregido por admin");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("editar permite al dueño modificar su propio reporte")
    void editar_duenioModificaSuPropio() {
        setAuthentication("rider@motovias.com");

        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("rider@motovias.com")).thenReturn(Optional.of(usuario));
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteUpdateDTO dto = new ReporteUpdateDTO();
        dto.setDescripcion("Actualizado por el dueño");
        dto.setEstado(EstadoPunto.RESUELTO);

        ReporteResponseDTO resultado = service.editar(1L, dto);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPunto.RESUELTO);
        verify(repository).save(any());
    }

    // ── Tests de auditoría (Historial de eventos) ─────────────────────────────

    @Test
    @DisplayName("crear registra un evento de auditoría de tipo CREACION")
    void crear_registraEventoDeCreacion() {
        PuntoInteres guardado = puntoGuardado(10L, Categoria.GOMERIA, EstadoPunto.ACTIVO);
        when(repository.save(any(PuntoInteres.class))).thenReturn(guardado);

        service.crear(reporteDTO("Gomería Norte", "Atiende 24hs", Categoria.GOMERIA, LAT, LON), usuario);

        verify(eventoRepository).save(argThat(evento ->
                evento.getTipoEvento() == TipoEventoReporte.CREACION
                        && evento.getUsuario() == usuario
                        && evento.getReporte() == guardado));
    }

    @Test
    @DisplayName("editar sin cambio de estado registra un evento de tipo EDICION")
    void editar_sinCambioDeEstado_registraEventoDeEdicion() {
        setAuthentication("rider@motovias.com");

        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("rider@motovias.com")).thenReturn(Optional.of(usuario));
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteUpdateDTO dto = new ReporteUpdateDTO();
        dto.setDescripcion("Corrijo la descripción");
        dto.setEstado(EstadoPunto.ACTIVO);

        service.editar(1L, dto);

        verify(eventoRepository).save(argThat(evento ->
                evento.getTipoEvento() == TipoEventoReporte.EDICION && evento.getUsuario() == usuario));
    }

    @Test
    @DisplayName("editar con cambio de estado registra un evento de tipo CAMBIO_ESTADO")
    void editar_conCambioDeEstado_registraEventoDeCambioEstado() {
        setAuthentication("rider@motovias.com");

        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(userRepository.findByEmail("rider@motovias.com")).thenReturn(Optional.of(usuario));
        when(repository.save(any(PuntoInteres.class))).thenAnswer(inv -> inv.getArgument(0));

        ReporteUpdateDTO dto = new ReporteUpdateDTO();
        dto.setDescripcion("Se resolvió");
        dto.setEstado(EstadoPunto.RESUELTO);

        service.editar(1L, dto);

        verify(eventoRepository).save(argThat(evento ->
                evento.getTipoEvento() == TipoEventoReporte.CAMBIO_ESTADO
                        && evento.getDescripcion().contains("ACTIVO")
                        && evento.getDescripcion().contains("RESUELTO")));
    }

    @Test
    @DisplayName("obtenerHistorial retorna los eventos mapeados en orden cronológico inverso")
    void obtenerHistorial_retornaEventosMapeados() {
        PuntoInteres punto = puntoGuardado(1L, Categoria.GOMERIA, EstadoPunto.ACTIVO);

        ReporteEvento evento1 = ReporteEvento.builder()
                .id(2L)
                .reporte(punto)
                .usuario(usuario)
                .tipoEvento(TipoEventoReporte.EDICION)
                .descripcion("Descripción actualizada")
                .timestamp(LocalDateTime.of(2026, 6, 12, 9, 0))
                .build();
        ReporteEvento evento2 = ReporteEvento.builder()
                .id(1L)
                .reporte(punto)
                .usuario(usuario)
                .tipoEvento(TipoEventoReporte.CREACION)
                .descripcion("Reporte creado")
                .timestamp(LocalDateTime.of(2026, 6, 11, 12, 0))
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(punto));
        when(eventoRepository.findByReporteOrderByTimestampDesc(punto)).thenReturn(List.of(evento1, evento2));

        List<ReporteEventoResponseDTO> historial = service.obtenerHistorial(1L);

        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getTipoEvento()).isEqualTo(TipoEventoReporte.EDICION);
        assertThat(historial.get(0).getNombreUsuario()).isEqualTo("Juan Perez");
        assertThat(historial.get(1).getTipoEvento()).isEqualTo(TipoEventoReporte.CREACION);
    }

    @Test
    @DisplayName("obtenerHistorial lanza 404 cuando el reporte no existe")
    void obtenerHistorial_reporteInexistente_lanza404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerHistorial(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(eventoRepository, never()).findByReporteOrderByTimestampDesc(any());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private void setAuthentication(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User crearUsuario(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setNombre("Test");
        return user;
    }

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
                .fuenteUbicacion(FuenteUbicacion.GPS)
                .ubicacion(GF.createPoint(new Coordinate(LON, LAT)))
                .fechaCreacion(LocalDateTime.of(2026, 6, 11, 12, 0))
                .build();
        p.setUsuario(usuario);
        return p;
    }
}
