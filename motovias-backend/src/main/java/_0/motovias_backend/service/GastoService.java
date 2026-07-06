package _0.motovias_backend.service;

import _0.motovias_backend.dto.GastoRequestDTO;
import _0.motovias_backend.dto.GastoResponseDTO;
import _0.motovias_backend.dto.TransferenciaSimplificadaDTO;
import _0.motovias_backend.model.Gasto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.model.ViajeParticipante;
import _0.motovias_backend.repository.GastoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.repository.ViajeParticipanteRepository;
import _0.motovias_backend.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository gastoRepository;
    private final ViajeRepository viajeRepository;
    private final ViajeParticipanteRepository participanteRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Umbral por debajo del cual un saldo se considera liquidado (< 1 centavo)
    private static final BigDecimal EPSILON = new BigDecimal("0.01");

    // ─── Escritura ──────────────────────────────────────────────────────────────

    @Transactional
    public GastoResponseDTO registrarGasto(Long viajeId, GastoRequestDTO dto, String emailUsuario) {
        Viaje viaje = findViajeOrThrow(viajeId);

        User pagador;
        if (dto.getPagadorId() != null) {
            pagador = userRepository.findById(dto.getPagadorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Pagador no encontrado"));
            validarMiembro(viaje, pagador);
        } else {
            pagador = findUserOrThrow(emailUsuario);
            validarMiembro(viaje, pagador);
        }

        Gasto gasto = Gasto.builder()
                .viaje(viaje)
                .pagador(pagador)
                .descripcion(dto.getDescripcion())
                .monto(dto.getMonto().setScale(2, RoundingMode.HALF_UP))
                .categoria(dto.getCategoria())
                .build();

        return toResponseDTO(gastoRepository.save(gasto));
    }

    // ─── Lectura ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GastoResponseDTO> listarGastos(Long viajeId) {
        Viaje viaje = findViajeOrThrow(viajeId);
        return gastoRepository.findByViajeOrderByFechaCreacionDesc(viaje)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Algoritmo greedy de liquidación de deudas (Debt Settlement).
     *
     * Calcula el balance neto de cada miembro activo (pagado - cuota equitativa)
     * y aplica un greedy de cruce entre el mayor deudor y el mayor acreedor
     * hasta agotar ambas colas, minimizando el número de transferencias.
     */
    @Transactional(readOnly = true)
    public List<TransferenciaSimplificadaDTO> calcularBalance(Long viajeId) {
        Viaje viaje = findViajeOrThrow(viajeId);

        // Reunir todos los miembros activos: creador + participantes
        List<User> miembros = new ArrayList<>();
        if (viaje.getCreador().isActivo()) {
            miembros.add(viaje.getCreador());
        }
        participanteRepository.findByViajeWithUsuario(viaje).stream()
                .map(ViajeParticipante::getUsuario)
                .filter(User::isActivo)
                .forEach(miembros::add);

        if (miembros.size() < 2) {
            return List.of();
        }

        List<Gasto> gastos = gastoRepository.findAllByViaje(viaje);
        if (gastos.isEmpty()) {
            return List.of();
        }

        // Cuota equitativa con alta precisión para minimizar error de redondeo
        int n = miembros.size();
        BigDecimal total = gastos.stream()
                .map(Gasto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cuota = total.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);

        // Monto pagado por cada usuario
        Map<Long, BigDecimal> pagadoPorId = new HashMap<>();
        for (Gasto g : gastos) {
            pagadoPorId.merge(g.getPagador().getId(), g.getMonto(), BigDecimal::add);
        }

        // Cola de prioridad: deudores (balance más negativo primero)
        PriorityQueue<UserBalance> deudores = new PriorityQueue<>(
                Comparator.comparing(UserBalance::balance));
        // Cola de prioridad: acreedores (balance más positivo primero)
        PriorityQueue<UserBalance> acreedores = new PriorityQueue<>(
                Comparator.comparing(UserBalance::balance).reversed());

        for (User miembro : miembros) {
            BigDecimal pagado = pagadoPorId.getOrDefault(miembro.getId(), BigDecimal.ZERO);
            BigDecimal balance = pagado.subtract(cuota);
            if (balance.compareTo(EPSILON.negate()) < 0) {
                deudores.add(new UserBalance(miembro, balance));
            } else if (balance.compareTo(EPSILON) > 0) {
                acreedores.add(new UserBalance(miembro, balance));
            }
        }

        List<TransferenciaSimplificadaDTO> transferencias = new ArrayList<>();

        while (!deudores.isEmpty() && !acreedores.isEmpty()) {
            UserBalance deudor = deudores.poll();
            UserBalance acreedor = acreedores.poll();

            BigDecimal deuda = deudor.balance().negate();
            BigDecimal credito = acreedor.balance();
            // El monto a transferir es el mínimo entre lo que debe y lo que se le debe
            BigDecimal monto = deuda.min(credito).setScale(2, RoundingMode.HALF_UP);

            transferencias.add(TransferenciaSimplificadaDTO.builder()
                    .deudorEmail(deudor.user().getEmail())
                    .deudorNombre(nombreCompleto(deudor.user()))
                    .acreedorEmail(acreedor.user().getEmail())
                    .acreedorNombre(nombreCompleto(acreedor.user()))
                    .monto(monto)
                    .build());

            // Actualizar saldos con el monto exacto transferido (redondeado)
            BigDecimal nuevoDeudorBalance = deudor.balance().add(monto);
            BigDecimal nuevoAcreedorBalance = acreedor.balance().subtract(monto);

            // Reinsertar solo si el saldo residual es significativo (>= 1 centavo)
            if (nuevoDeudorBalance.compareTo(EPSILON.negate()) < 0) {
                deudores.add(new UserBalance(deudor.user(), nuevoDeudorBalance));
            }
            if (nuevoAcreedorBalance.compareTo(EPSILON) > 0) {
                acreedores.add(new UserBalance(acreedor.user(), nuevoAcreedorBalance));
            }
        }

        return transferencias;
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Viaje findViajeOrThrow(Long viajeId) {
        return viajeRepository.findById(viajeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Viaje no encontrado con id: " + viajeId));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"));
    }

    private void validarMiembro(Viaje viaje, User usuario) {
        boolean esCreador = viaje.getCreador().getId().equals(usuario.getId());
        boolean esParticipante = participanteRepository.existsByViajeAndUsuario(viaje, usuario);
        if (!esCreador && !esParticipante) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El pagador no pertenece al viaje");
        }
    }

    private GastoResponseDTO toResponseDTO(Gasto g) {
        return GastoResponseDTO.builder()
                .id(g.getId())
                .descripcion(g.getDescripcion())
                .monto(g.getMonto())
                .categoria(g.getCategoria())
                .fechaCreacion(g.getFechaCreacion() != null
                        ? g.getFechaCreacion().format(ISO_FMT) : null)
                .pagadorNombre(nombreCompleto(g.getPagador()))
                .pagadorEmail(g.getPagador().getEmail())
                .build();
    }

    private String nombreCompleto(User u) {
        String nombre = u.getNombre();
        if (u.getApellido() != null && !u.getApellido().isBlank()) {
            nombre += " " + u.getApellido();
        }
        return nombre;
    }

    private record UserBalance(User user, BigDecimal balance) {}
}
