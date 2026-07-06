package _0.motovias_backend.service;

import _0.motovias_backend.dto.ViajeRequestDTO;
import _0.motovias_backend.dto.ViajeResponseDTO;
import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.model.ViajeParticipante;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.GastoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.repository.ViajeParticipanteRepository;
import _0.motovias_backend.repository.ViajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViajeServiceTest {

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private ViajeParticipanteRepository participanteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private ViajeService viajeService;

    // ── Generación de código ──────────────────────────────────────────────────

    @Test
    @DisplayName("generarCodigoUnico retorna código de 6 caracteres alfanuméricos en mayúsculas")
    void generarCodigoUnico_formatoCorrecto() {
        when(viajeRepository.existsByCodigo(anyString())).thenReturn(false);

        String codigo = viajeService.generarCodigoUnico();

        assertThat(codigo).hasSize(6);
        assertThat(codigo).matches("[A-Z2-9]{6}");
    }

    @Test
    @DisplayName("generarCodigoUnico no contiene caracteres confusos O, 0, I ni 1")
    void generarCodigoUnico_sinCaracteresConfusos() {
        when(viajeRepository.existsByCodigo(anyString())).thenReturn(false);

        // Generar muchos códigos y verificar que ninguno tenga O, 0, I ni 1
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codigos.add(viajeService.generarCodigoUnico());
        }

        codigos.forEach(c ->
                assertThat(c).doesNotContain("O", "0", "I", "1")
        );
    }

    @Test
    @DisplayName("generarCodigoUnico reintenta cuando el primer código ya existe en BD")
    void generarCodigoUnico_reintetaEnColision() {
        // Primeras dos llamadas devuelven colisión, la tercera es única
        when(viajeRepository.existsByCodigo(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        String codigo = viajeService.generarCodigoUnico();

        assertThat(codigo).hasSize(6);
        verify(viajeRepository, times(3)).existsByCodigo(anyString());
    }

    @Test
    @DisplayName("crearViaje genera código único y persiste el viaje")
    void crearViaje_persisteConCodigoUnico() {
        User creador = usuarioConId(1L, "juan@test.com", "Juan", null);
        ViajeRequestDTO dto = new ViajeRequestDTO();
        dto.setTitulo("Ruta de las Sierras");
        dto.setDescripcion("Viaje por Córdoba");

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(creador));
        when(viajeRepository.existsByCodigo(anyString())).thenReturn(false);
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> {
            Viaje v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        ViajeResponseDTO resultado = viajeService.crearViaje(dto, "juan@test.com");

        assertThat(resultado.getTitulo()).isEqualTo("Ruta de las Sierras");
        assertThat(resultado.getCodigo()).hasSize(6);
        assertThat(resultado.getOrganizadorEmail()).isEqualTo("juan@test.com");
        verify(viajeRepository).save(any(Viaje.class));
    }

    // ── Límite de 20 participantes ────────────────────────────────────────────

    @Test
    @DisplayName("unirseAViaje lanza 400 cuando el viaje ya tiene 20 participantes")
    void unirseAViaje_rechazaCuandoLimiteLlegado() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User participante = usuarioConId(2L, "nuevo@test.com", "Nuevo", null);
        Viaje viaje = viajeConCreador("ABC123", organizador);

        when(viajeRepository.findByCodigo("ABC123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(participante));
        when(participanteRepository.existsByViajeAndUsuario(viaje, participante)).thenReturn(false);
        when(participanteRepository.countByViaje(viaje)).thenReturn(20L);

        assertThatThrownBy(() -> viajeService.unirseAViaje("ABC123", "nuevo@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("20");
    }

    @Test
    @DisplayName("unirseAViaje permite unirse cuando hay exactamente 19 participantes")
    void unirseAViaje_aceptaConParticipanteNro20() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User participante = usuarioConId(2L, "nuevo@test.com", "Nuevo", null);
        Viaje viaje = viajeConCreador("ABC123", organizador);

        when(viajeRepository.findByCodigo("ABC123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(participante));
        when(participanteRepository.existsByViajeAndUsuario(viaje, participante)).thenReturn(false);
        when(participanteRepository.countByViaje(viaje)).thenReturn(19L);
        when(participanteRepository.save(any(ViajeParticipante.class))).thenReturn(new ViajeParticipante());

        ViajeResponseDTO resultado = viajeService.unirseAViaje("ABC123", "nuevo@test.com");

        assertThat(resultado).isNotNull();
        verify(participanteRepository).save(any(ViajeParticipante.class));
    }

    // ── Bloqueo de participantes duplicados ───────────────────────────────────

    @Test
    @DisplayName("unirseAViaje lanza 400 cuando el usuario ya es participante")
    void unirseAViaje_rechazaParticipanteDuplicado() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User participante = usuarioConId(2L, "ya@test.com", "Ya", null);
        Viaje viaje = viajeConCreador("XYZ789", organizador);

        when(viajeRepository.findByCodigo("XYZ789")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("ya@test.com")).thenReturn(Optional.of(participante));
        when(participanteRepository.existsByViajeAndUsuario(viaje, participante)).thenReturn(true);

        assertThatThrownBy(() -> viajeService.unirseAViaje("XYZ789", "ya@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("participante");
    }

    @Test
    @DisplayName("unirseAViaje lanza 400 cuando el usuario es el organizador del viaje")
    void unirseAViaje_rechazaAlOrganizador() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        Viaje viaje = viajeConCreador("ORG123", organizador);

        when(viajeRepository.findByCodigo("ORG123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizador));

        assertThatThrownBy(() -> viajeService.unirseAViaje("ORG123", "org@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("organizador");
    }

    @Test
    @DisplayName("unirseAViaje lanza 404 cuando el código no existe")
    void unirseAViaje_lanza404CodigoInexistente() {
        when(viajeRepository.findByCodigo("NOPE99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.unirseAViaje("NOPE99", "user@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("NOPE99");
    }

    @Test
    @DisplayName("buscarPorCodigo lanza 404 cuando el código no existe")
    void buscarPorCodigo_lanza404Inexistente() {
        when(viajeRepository.findByCodigo("XXXXXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.buscarPorCodigo("XXXXXX"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("XXXXXX");
    }

    @Test
    @DisplayName("buscarPorCodigo normaliza el código a mayúsculas antes de buscar")
    void buscarPorCodigo_normalizaAMayusculas() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        Viaje viaje = viajeConCreador("ABC123", organizador);

        when(viajeRepository.findByCodigo("ABC123")).thenReturn(Optional.of(viaje));

        ViajeResponseDTO resultado = viajeService.buscarPorCodigo("abc123");

        assertThat(resultado.getCodigo()).isEqualTo("ABC123");
        verify(viajeRepository).findByCodigo("ABC123");
    }

    // ── Viaje activo ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerViajeActivo retorna vacío cuando el usuario no participa de ningún viaje")
    void obtenerViajeActivo_retornaVacioSinViajes() {
        User usuario = usuarioConId(1L, "sinviaje@test.com", "Sin Viaje", null);

        when(userRepository.findByEmail("sinviaje@test.com")).thenReturn(Optional.of(usuario));
        when(viajeRepository.findTopByCreadorOrderByFechaCreacionDesc(usuario)).thenReturn(Optional.empty());
        when(participanteRepository.findTopByUsuarioOrderByFechaUnionDesc(usuario)).thenReturn(Optional.empty());

        Optional<ViajeResponseDTO> resultado = viajeService.obtenerViajeActivo("sinviaje@test.com");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("obtenerViajeActivo retorna el viaje creado cuando el usuario es organizador")
    void obtenerViajeActivo_retornaViajeComoCreador() {
        User creador = usuarioConId(1L, "org@test.com", "Org", null);
        Viaje viaje = viajeConCreador("ORG123", creador);
        viaje.setFechaCreacion(LocalDateTime.now());

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(creador));
        when(viajeRepository.findTopByCreadorOrderByFechaCreacionDesc(creador)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findTopByUsuarioOrderByFechaUnionDesc(creador)).thenReturn(Optional.empty());

        Optional<ViajeResponseDTO> resultado = viajeService.obtenerViajeActivo("org@test.com");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("ORG123");
    }

    @Test
    @DisplayName("obtenerViajeActivo elige el viaje más reciente cuando el usuario es creador de uno y participante de otro")
    void obtenerViajeActivo_eligeElMasReciente() {
        User usuario = usuarioConId(1L, "user@test.com", "User", null);
        User otroCreador = usuarioConId(2L, "otro@test.com", "Otro", null);

        Viaje viajeCreado = viajeConCreador("VIEJO1", usuario);
        viajeCreado.setFechaCreacion(LocalDateTime.now().minusDays(5));

        Viaje viajeUnido = viajeConCreador("NUEVO1", otroCreador);
        ViajeParticipante participacion = ViajeParticipante.builder()
                .viaje(viajeUnido)
                .usuario(usuario)
                .fechaUnion(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(usuario));
        when(viajeRepository.findTopByCreadorOrderByFechaCreacionDesc(usuario)).thenReturn(Optional.of(viajeCreado));
        when(participanteRepository.findTopByUsuarioOrderByFechaUnionDesc(usuario)).thenReturn(Optional.of(participacion));

        Optional<ViajeResponseDTO> resultado = viajeService.obtenerViajeActivo("user@test.com");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getCodigo()).isEqualTo("NUEVO1");
    }

    // ── Abandonar caravana ───────────────────────────────────────────────────

    @Test
    @DisplayName("salirDeViaje elimina la participación del usuario")
    void salirDeViaje_eliminaParticipante() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User participante = usuarioConId(2L, "part@test.com", "Part", null);
        Viaje viaje = viajeConCreador("SAL123", organizador);
        ViajeParticipante participacion = ViajeParticipante.builder()
                .viaje(viaje)
                .usuario(participante)
                .build();

        when(viajeRepository.findByCodigo("SAL123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("part@test.com")).thenReturn(Optional.of(participante));
        when(participanteRepository.findByViajeAndUsuario(viaje, participante)).thenReturn(Optional.of(participacion));

        viajeService.salirDeViaje("SAL123", "part@test.com");

        verify(participanteRepository).delete(participacion);
    }

    @Test
    @DisplayName("salirDeViaje lanza 400 cuando el organizador intenta abandonar su propia caravana")
    void salirDeViaje_rechazaAlOrganizador() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        Viaje viaje = viajeConCreador("ORG999", organizador);

        when(viajeRepository.findByCodigo("ORG999")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizador));

        assertThatThrownBy(() -> viajeService.salirDeViaje("ORG999", "org@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("organizador");
    }

    @Test
    @DisplayName("salirDeViaje lanza 400 cuando el usuario no es participante de la caravana")
    void salirDeViaje_rechazaSiNoEsParticipante() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User ajeno = usuarioConId(3L, "ajeno@test.com", "Ajeno", null);
        Viaje viaje = viajeConCreador("AJE123", organizador);

        when(viajeRepository.findByCodigo("AJE123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("ajeno@test.com")).thenReturn(Optional.of(ajeno));
        when(participanteRepository.findByViajeAndUsuario(viaje, ajeno)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viajeService.salirDeViaje("AJE123", "ajeno@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("participante");
    }

    // ── Eliminar caravana (organizador) ───────────────────────────────────────

    @Test
    @DisplayName("eliminarViaje borra gastos, participantes y el viaje cuando lo pide el organizador")
    void eliminarViaje_organizador_borraEnCascada() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        Viaje viaje = viajeConCreador("DEL123", organizador);

        when(viajeRepository.findByCodigo("DEL123")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizador));

        viajeService.eliminarViaje("DEL123", "org@test.com");

        verify(gastoRepository).deleteByViaje(viaje);
        verify(participanteRepository).deleteByViaje(viaje);
        verify(viajeRepository).delete(viaje);
    }

    @Test
    @DisplayName("eliminarViaje lanza 403 cuando lo pide un participante que no es el organizador")
    void eliminarViaje_noOrganizador_lanza403() {
        User organizador = usuarioConId(1L, "org@test.com", "Org", null);
        User participante = usuarioConId(2L, "part@test.com", "Part", null);
        Viaje viaje = viajeConCreador("DEL999", organizador);

        when(viajeRepository.findByCodigo("DEL999")).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("part@test.com")).thenReturn(Optional.of(participante));

        assertThatThrownBy(() -> viajeService.eliminarViaje("DEL999", "part@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("organizador");

        verify(viajeRepository, never()).delete(any());
        verify(gastoRepository, never()).deleteByViaje(any());
        verify(participanteRepository, never()).deleteByViaje(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User usuarioConId(Long id, String email, String nombre, String apellido) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(nombre);
        u.setApellido(apellido);
        return u;
    }

    private Viaje viajeConCreador(String codigo, User creador) {
        return Viaje.builder()
                .id(1L)
                .titulo("Viaje test")
                .codigo(codigo)
                .creador(creador)
                .build();
    }
}
