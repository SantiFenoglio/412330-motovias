package _0.motovias_backend.service;

import _0.motovias_backend.dto.GastoRequestDTO;
import _0.motovias_backend.dto.GastoResponseDTO;
import _0.motovias_backend.dto.TransferenciaSimplificadaDTO;
import _0.motovias_backend.model.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock private GastoRepository gastoRepository;
    @Mock private ViajeRepository viajeRepository;
    @Mock private ViajeParticipanteRepository participanteRepository;
    @Mock private UserRepository userRepository;
    @Mock private MercadoPagoService mercadoPagoService;

    @InjectMocks private GastoService gastoService;

    // ═══════════════════════════════════════════════════════════════════════════
    // registrarGasto
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("registrarGasto persiste el gasto con el usuario autenticado como pagador")
    void registrarGasto_pagadorAutenticado_persiste() {
        User creador = user(1L, "a@test.com", "Alice", null);
        Viaje viaje = viaje(1L, creador);
        GastoRequestDTO dto = dto("Nafta", "80.00", CategoriaGasto.COMBUSTIBLE, null);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(creador));
        when(participanteRepository.existsByViajeAndUsuario(viaje, creador)).thenReturn(false); // es creador
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> {
            Gasto g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        GastoResponseDTO resultado = gastoService.registrarGasto(1L, dto, "a@test.com");

        assertThat(resultado.getDescripcion()).isEqualTo("Nafta");
        assertThat(resultado.getMonto()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resultado.getPagadorEmail()).isEqualTo("a@test.com");
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    @DisplayName("registrarGasto con pagadorId explícito usa ese usuario")
    void registrarGasto_pagadorIdExplicito_usaEseUsuario() {
        User creador = user(1L, "org@test.com", "Org", null);
        User participante = user(2L, "part@test.com", "Part", null);
        Viaje viaje = viaje(1L, creador);
        GastoRequestDTO dto = dto("Peaje", "150.00", CategoriaGasto.PEAJE, 2L);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participante));
        when(participanteRepository.existsByViajeAndUsuario(viaje, participante)).thenReturn(true);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> {
            Gasto g = inv.getArgument(0);
            g.setId(20L);
            return g;
        });

        GastoResponseDTO resultado = gastoService.registrarGasto(1L, dto, "org@test.com");

        assertThat(resultado.getPagadorEmail()).isEqualTo("part@test.com");
    }

    @Test
    @DisplayName("registrarGasto lanza 404 cuando el viaje no existe")
    void registrarGasto_viajeInexistente_lanza404() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gastoService.registrarGasto(99L, dto("X", "10.00", CategoriaGasto.OTRO, null), "x@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("registrarGasto lanza 403 cuando el pagadorId no es miembro del viaje")
    void registrarGasto_pagadorNoMiembro_lanza403() {
        User creador = user(1L, "org@test.com", "Org", null);
        User externo = user(99L, "ext@test.com", "Ext", null);
        Viaje viaje = viaje(1L, creador);
        GastoRequestDTO dto = dto("Hotel", "500.00", CategoriaGasto.ALOJAMIENTO, 99L);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(userRepository.findById(99L)).thenReturn(Optional.of(externo));
        when(participanteRepository.existsByViajeAndUsuario(viaje, externo)).thenReturn(false);

        assertThatThrownBy(() -> gastoService.registrarGasto(1L, dto, "org@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pertenece");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // calcularBalance — casos base
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("calcularBalance retorna vacío cuando no hay gastos registrados")
    void calcularBalance_sinGastos_retornaVacio() {
        User creador = user(1L, "a@test.com", "Alice", null);
        Viaje viaje = viaje(1L, creador);
        User b = user(2L, "b@test.com", "Bob", null);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje)).thenReturn(List.of(participante(viaje, b)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of());

        assertThat(gastoService.calcularBalance(1L)).isEmpty();
    }

    @Test
    @DisplayName("calcularBalance retorna vacío cuando solo hay un miembro activo")
    void calcularBalance_unMiembro_retornaVacio() {
        User creador = user(1L, "a@test.com", "Alice", null);
        Viaje viaje = viaje(1L, creador);
        // Sin participantes adicionales

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje)).thenReturn(List.of());
        // no hace falta llamar a gastoRepository porque retorna antes

        assertThat(gastoService.calcularBalance(1L)).isEmpty();
    }

    @Test
    @DisplayName("calcularBalance lanza 404 cuando el viaje no existe")
    void calcularBalance_viajeInexistente_lanza404() {
        when(viajeRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gastoService.calcularBalance(77L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("77");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // calcularBalance — algoritmo greedy
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("dos personas: quien no pagó le debe al que pagó todo la mitad exacta")
    void calcularBalance_dosPersonas_unaTransferencia() {
        // Total=100, n=2, cuota=50 → A: +50, B: -50
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje)).thenReturn(List.of(participante(viaje, b)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(gasto(viaje, a, "100.00")));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeudorEmail()).isEqualTo("b@test.com");
        assertThat(result.get(0).getAcreedorEmail()).isEqualTo("a@test.com");
        assertThat(result.get(0).getMonto()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("tres personas, un solo pagador: los otros dos le deben partes iguales")
    void calcularBalance_tresPersonas_dosTransferencias() {
        // Total=150, n=3, cuota=50 → A: +100, B: -50, C: -50
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(gasto(viaje, a, "150.00")));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TransferenciaSimplificadaDTO::getAcreedorEmail)
                .containsOnly("a@test.com");
        assertThat(result).extracting(TransferenciaSimplificadaDTO::getMonto)
                .allMatch(m -> m.compareTo(new BigDecimal("50.00")) == 0);
        assertThat(result).extracting(TransferenciaSimplificadaDTO::getDeudorEmail)
                .containsExactlyInAnyOrder("b@test.com", "c@test.com");
    }

    @Test
    @DisplayName("ciclo triangular puro A→B→C→A: pagos iguales se cancelan (0 transferencias)")
    void calcularBalance_cicloTriangularPuro_ceroTransacciones() {
        // Caso canónico del enunciado: "A le debe a B, B le debe a C, C le debe a A"
        // Si cada uno pagó la misma cuota, el neto es 0 → mínimo absoluto = 0 movimientos
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c)));
        // Total=270, cuota=90: todos pagaron exactamente su cuota
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(
                gasto(viaje, a, "90.00"),
                gasto(viaje, b, "90.00"),
                gasto(viaje, c, "90.00")
        ));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        // El ciclo se cancela: 0 transacciones (mínimo matemático)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ciclo asimétrico: un pagador absorbió más, se resuelve en una sola transferencia")
    void calcularBalance_cicloAsimetrico_unaTransferenciaOptima() {
        // A pagó el doble que B y C no pagó nada
        // Total=360, n=3, cuota=120 → A: +120, B: 0, C: -120
        // Mínimo: 1 transferencia (C→A)
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(
                gasto(viaje, a, "240.00"),
                gasto(viaje, b, "120.00")
        ));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeudorEmail()).isEqualTo("c@test.com");
        assertThat(result.get(0).getAcreedorEmail()).isEqualTo("a@test.com");
        assertThat(result.get(0).getMonto()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    @DisplayName("cuatro personas con pagos disparejos: greedy produce el mínimo de transacciones")
    void calcularBalance_cuatroPersonas_tresPagadores_minimoTransacciones() {
        // A=200, B=100, C=50, D=0. Total=350, n=4, cuota=87.50
        // A: +112.50, B: +12.50, C: -37.50, D: -87.50
        // Greedy óptimo: D→A 87.50 | C→A 25.00 | C→B 12.50 → 3 transacciones
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        User d = user(4L, "d@test.com", "Diana", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c), participante(viaje, d)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(
                gasto(viaje, a, "200.00"),
                gasto(viaje, b, "100.00"),
                gasto(viaje, c, "50.00")
        ));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(3);

        // Primera transferencia: D (mayor deudor) → A (mayor acreedor)
        assertThat(result.get(0).getDeudorEmail()).isEqualTo("d@test.com");
        assertThat(result.get(0).getAcreedorEmail()).isEqualTo("a@test.com");
        assertThat(result.get(0).getMonto()).isEqualByComparingTo(new BigDecimal("87.50"));

        // Segunda transferencia: C → A (absorbe el resto de A)
        assertThat(result.get(1).getDeudorEmail()).isEqualTo("c@test.com");
        assertThat(result.get(1).getAcreedorEmail()).isEqualTo("a@test.com");
        assertThat(result.get(1).getMonto()).isEqualByComparingTo(new BigDecimal("25.00"));

        // Tercera transferencia: C → B (salda el resto de C con B)
        assertThat(result.get(2).getDeudorEmail()).isEqualTo("c@test.com");
        assertThat(result.get(2).getAcreedorEmail()).isEqualTo("b@test.com");
        assertThat(result.get(2).getMonto()).isEqualByComparingTo(new BigDecimal("12.50"));
    }

    @Test
    @DisplayName("verificación de conservación: la suma de lo que paga cada deudor == su deuda real")
    void calcularBalance_conservacionDeMontos_deudaCubiertaExactamente() {
        // D debe 87.50: paga 87.50 en una transacción → queda saldado
        // C debe 37.50: paga 25.00 + 12.50 = 37.50 → queda saldado
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        User d = user(4L, "d@test.com", "Diana", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c), participante(viaje, d)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(
                gasto(viaje, a, "200.00"),
                gasto(viaje, b, "100.00"),
                gasto(viaje, c, "50.00")
        ));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        // D pagó $0, cuota $87.50 → debe $87.50
        BigDecimal sumadoPorD = result.stream()
                .filter(t -> "d@test.com".equals(t.getDeudorEmail()))
                .map(TransferenciaSimplificadaDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumadoPorD).isEqualByComparingTo(new BigDecimal("87.50"));

        // C pagó $50, cuota $87.50 → debe $37.50
        BigDecimal sumadoPorC = result.stream()
                .filter(t -> "c@test.com".equals(t.getDeudorEmail()))
                .map(TransferenciaSimplificadaDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumadoPorC).isEqualByComparingTo(new BigDecimal("37.50"));
    }

    @Test
    @DisplayName("precisión de dos decimales en montos no divisibles exactamente")
    void calcularBalance_precisionDecimal_dosDecimales() {
        // Total=100, n=3 → cuota=33.33... → cada deudor paga 33.33
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        User c = user(3L, "c@test.com", "Carlos", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje))
                .thenReturn(List.of(participante(viaje, b), participante(viaje, c)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(gasto(viaje, a, "100.00")));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(2);
        result.forEach(t -> {
            // Todos los montos deben tener exactamente 2 decimales
            assertThat(t.getMonto().scale()).isEqualTo(2);
            assertThat(t.getMonto()).isEqualByComparingTo(new BigDecimal("33.33"));
        });
    }

    @Test
    @DisplayName("todos los saldos ya están equilibrados: cero transferencias necesarias")
    void calcularBalance_todosEquilibrados_ceroTransacciones() {
        // Cada persona pagó exactamente su cuota
        User a = user(1L, "a@test.com", "Alice", null);
        User b = user(2L, "b@test.com", "Bob", null);
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje)).thenReturn(List.of(participante(viaje, b)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(
                gasto(viaje, a, "60.00"),
                gasto(viaje, b, "60.00")
        ));

        assertThat(gastoService.calcularBalance(1L)).isEmpty();
    }

    @Test
    @DisplayName("el DTO incluye nombre y email tanto del deudor como del acreedor")
    void calcularBalance_dtoContieneNombreYEmail() {
        User a = user(1L, "alice@test.com", "Alice", "Smith");
        User b = user(2L, "bob@test.com", "Bob", "Jones");
        Viaje viaje = viaje(1L, a);

        when(viajeRepository.findById(1L)).thenReturn(Optional.of(viaje));
        when(participanteRepository.findByViajeWithUsuario(viaje)).thenReturn(List.of(participante(viaje, b)));
        when(gastoRepository.findAllByViaje(viaje)).thenReturn(List.of(gasto(viaje, a, "200.00")));

        List<TransferenciaSimplificadaDTO> result = gastoService.calcularBalance(1L);

        assertThat(result).hasSize(1);
        TransferenciaSimplificadaDTO t = result.get(0);
        assertThat(t.getDeudorEmail()).isEqualTo("bob@test.com");
        assertThat(t.getDeudorNombre()).isEqualTo("Bob Jones");
        assertThat(t.getAcreedorEmail()).isEqualTo("alice@test.com");
        assertThat(t.getAcreedorNombre()).isEqualTo("Alice Smith");
        assertThat(t.getMonto()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private User user(Long id, String email, String nombre, String apellido) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setActivo(true);
        return u;
    }

    private Viaje viaje(Long id, User creador) {
        return Viaje.builder()
                .id(id)
                .titulo("Viaje test")
                .codigo("TEST01")
                .creador(creador)
                .build();
    }

    private ViajeParticipante participante(Viaje viaje, User usuario) {
        return ViajeParticipante.builder()
                .viaje(viaje)
                .usuario(usuario)
                .build();
    }

    private Gasto gasto(Viaje viaje, User pagador, String monto) {
        return Gasto.builder()
                .viaje(viaje)
                .pagador(pagador)
                .monto(new BigDecimal(monto))
                .descripcion("Test")
                .categoria(CategoriaGasto.OTRO)
                .build();
    }

    private GastoRequestDTO dto(String descripcion, String monto, CategoriaGasto categoria, Long pagadorId) {
        GastoRequestDTO dto = new GastoRequestDTO();
        dto.setDescripcion(descripcion);
        dto.setMonto(new BigDecimal(monto));
        dto.setCategoria(categoria);
        dto.setPagadorId(pagadorId);
        return dto;
    }
}
