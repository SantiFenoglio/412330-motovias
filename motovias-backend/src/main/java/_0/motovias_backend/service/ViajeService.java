package _0.motovias_backend.service;

import _0.motovias_backend.dto.ParticipanteResponseDTO;
import _0.motovias_backend.dto.ViajeRequestDTO;
import _0.motovias_backend.dto.ViajeResponseDTO;
import _0.motovias_backend.model.Viaje;
import _0.motovias_backend.model.ViajeParticipante;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.GastoRepository;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.repository.ViajeParticipanteRepository;
import _0.motovias_backend.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final ViajeParticipanteRepository participanteRepository;
    private final UserRepository userRepository;
    private final GastoRepository gastoRepository;

    // Excluye O/0 e I/1 para evitar confusión visual
    private static final String CODIGO_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODIGO_LENGTH = 6;
    static final int MAX_PARTICIPANTES = 20;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public ViajeResponseDTO crearViaje(ViajeRequestDTO dto, String emailCreador) {
        User creador = findUserOrThrow(emailCreador);

        Viaje viaje = Viaje.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .codigo(generarCodigoUnico())
                .creador(creador)
                .build();

        return toDTO(viajeRepository.save(viaje));
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponseDTO> listarParticipantes(Long viajeId) {
        Viaje viaje = viajeRepository.findById(viajeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Viaje no encontrado con id: " + viajeId));

        List<ParticipanteResponseDTO> resultado = new ArrayList<>();

        resultado.add(buildParticipanteDTO(viaje.getCreador()));

        participanteRepository.findByViajeWithUsuario(viaje).stream()
                .map(ViajeParticipante::getUsuario)
                .filter(User::isActivo)
                .map(this::buildParticipanteDTO)
                .forEach(resultado::add);

        return resultado;
    }

    public ViajeResponseDTO buscarPorCodigo(String codigo) {
        return toDTO(findViajeOrThrow(codigo));
    }

    @Transactional
    public ViajeResponseDTO unirseAViaje(String codigo, String emailUsuario) {
        Viaje viaje = findViajeOrThrow(codigo);
        User usuario = findUserOrThrow(emailUsuario);

        if (viaje.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El organizador no puede unirse como participante");
        }

        if (participanteRepository.existsByViajeAndUsuario(viaje, usuario)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya eres participante de este viaje");
        }

        if (participanteRepository.countByViaje(viaje) >= MAX_PARTICIPANTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El viaje ya alcanzó el límite de " + MAX_PARTICIPANTES + " participantes");
        }

        participanteRepository.save(ViajeParticipante.builder()
                .viaje(viaje)
                .usuario(usuario)
                .build());

        return toDTO(viaje);
    }

    @Transactional(readOnly = true)
    public Optional<ViajeResponseDTO> obtenerViajeActivo(String email) {
        User usuario = findUserOrThrow(email);

        Optional<Viaje> comoCreador = viajeRepository.findTopByCreadorOrderByFechaCreacionDesc(usuario);
        Optional<ViajeParticipante> comoParticipante =
                participanteRepository.findTopByUsuarioOrderByFechaUnionDesc(usuario);

        if (comoCreador.isEmpty() && comoParticipante.isEmpty()) {
            return Optional.empty();
        }
        if (comoCreador.isEmpty()) {
            return Optional.of(toDTO(comoParticipante.get().getViaje()));
        }
        if (comoParticipante.isEmpty()) {
            return Optional.of(toDTO(comoCreador.get()));
        }

        boolean creadorEsMasReciente = comoCreador.get().getFechaCreacion()
                .isAfter(comoParticipante.get().getFechaUnion());
        Viaje viajeActivo = creadorEsMasReciente ? comoCreador.get() : comoParticipante.get().getViaje();
        return Optional.of(toDTO(viajeActivo));
    }

    @Transactional
    public void salirDeViaje(String codigo, String emailUsuario) {
        Viaje viaje = findViajeOrThrow(codigo);
        User usuario = findUserOrThrow(emailUsuario);

        if (viaje.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El organizador no puede abandonar su propia caravana");
        }

        ViajeParticipante participante = participanteRepository.findByViajeAndUsuario(viaje, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No eres participante de esta caravana"));

        participanteRepository.delete(participante);
    }

    @Transactional
    public void eliminarViaje(String codigo, String emailUsuario) {
        Viaje viaje = findViajeOrThrow(codigo);
        User usuario = findUserOrThrow(emailUsuario);

        if (!viaje.getCreador().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el organizador puede eliminar la caravana");
        }

        gastoRepository.deleteByViaje(viaje);
        participanteRepository.deleteByViaje(viaje);
        viajeRepository.delete(viaje);
    }

    // Genera un código único garantizado contra colisiones en BD
    String generarCodigoUnico() {
        String codigo;
        do {
            codigo = generarCodigo();
        } while (viajeRepository.existsByCodigo(codigo));
        return codigo;
    }

    private String generarCodigo() {
        StringBuilder sb = new StringBuilder(CODIGO_LENGTH);
        for (int i = 0; i < CODIGO_LENGTH; i++) {
            sb.append(CODIGO_CHARS.charAt(RANDOM.nextInt(CODIGO_CHARS.length())));
        }
        return sb.toString();
    }

    private Viaje findViajeOrThrow(String codigo) {
        return viajeRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Viaje no encontrado con código: " + codigo));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"));
    }

    private ParticipanteResponseDTO buildParticipanteDTO(User u) {
        String nombre = u.getNombre();
        String apellido = u.getApellido();
        if (apellido != null && !apellido.isBlank()) {
            nombre += " " + apellido;
        }
        return ParticipanteResponseDTO.builder()
                .id(u.getId())
                .nombre(nombre)
                .email(u.getEmail())
                .build();
    }

    private ViajeResponseDTO toDTO(Viaje v) {
        String nombreOrganizador = v.getCreador().getNombre();
        String apellido = v.getCreador().getApellido();
        if (apellido != null && !apellido.isBlank()) {
            nombreOrganizador += " " + apellido;
        }

        return ViajeResponseDTO.builder()
                .id(v.getId())
                .titulo(v.getTitulo())
                .descripcion(v.getDescripcion())
                .fechaCreacion(v.getFechaCreacion() != null ? v.getFechaCreacion().format(ISO_FMT) : null)
                .codigo(v.getCodigo())
                .organizadorNombre(nombreOrganizador)
                .organizadorEmail(v.getCreador().getEmail())
                .build();
    }
}
