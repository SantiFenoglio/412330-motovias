package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.dto.ReporteUpdateDTO;
import _0.motovias_backend.dto.VotoRequestDTO;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteVoto;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.TipoVoto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteVotoRepository;
import _0.motovias_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PuntoInteresRepository repository;
    private final UserRepository userRepository;
    private final ReporteVotoRepository votoRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<ReporteResponseDTO> listarMios(User usuario) {
        return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ReporteResponseDTO crear(ReporteRequestDTO dto, User usuario) {
        Point ubicacion = GF.createPoint(new Coordinate(dto.getLongitud(), dto.getLatitud()));

        PuntoInteres entidad = PuntoInteres.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .categoria(dto.getCategoria())
                .ubicacion(ubicacion)
                .usuario(usuario)
                .build();

        return toDTO(repository.save(entidad));
    }

    public ReporteResponseDTO editar(Long id, ReporteUpdateDTO dto) {
        PuntoInteres punto = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        validarAutoria(punto);

        punto.setDescripcion(dto.getDescripcion());
        punto.setEstado(dto.getEstado());

        return toDTO(repository.save(punto));
    }

    public void eliminar(Long id) {
        PuntoInteres punto = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        validarAutoria(punto);

        repository.delete(punto);
    }

    @Transactional
    public ReporteResponseDTO votar(Long reporteId, VotoRequestDTO dto, User usuario) {
        PuntoInteres punto = repository.findById(reporteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        if (punto.getUsuario() != null && punto.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés votar tu propio reporte");
        }

        Optional<ReporteVoto> votoExistente = votoRepository.findByUsuarioAndReporte(usuario, punto);

        if (votoExistente.isPresent()) {
            ReporteVoto voto = votoExistente.get();
            if (voto.getTipoVoto() == dto.getTipoVoto()) {
                votoRepository.delete(voto);
            } else {
                voto.setTipoVoto(dto.getTipoVoto());
                votoRepository.save(voto);
            }
        } else {
            votoRepository.save(ReporteVoto.builder()
                    .usuario(usuario)
                    .reporte(punto)
                    .tipoVoto(dto.getTipoVoto())
                    .build());
        }

        long confirmaciones = votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA);
        long refutaciones = votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA);
        long balance = confirmaciones - refutaciones;

        if (balance <= -5 && punto.getEstado() != EstadoPunto.DUDOSO) {
            punto.setEstado(EstadoPunto.DUDOSO);
            repository.save(punto);
        }

        ReporteResponseDTO response = toDTO(punto);
        response.setConfirmaciones(confirmaciones);
        response.setRefutaciones(refutaciones);
        return response;
    }

    private void validarAutoria(PuntoInteres punto) {
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        User autenticado = userRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean esDuenio = punto.getUsuario() != null
                && emailAutenticado.equals(punto.getUsuario().getEmail());
        boolean esAdmin = Role.ADMIN.equals(autenticado.getRole());

        if (!esDuenio && !esAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permiso para modificar este reporte");
        }
    }

    ReporteResponseDTO toDTO(PuntoInteres p) {
        String nombreUsuario = null;
        if (p.getUsuario() != null) {
            nombreUsuario = p.getUsuario().getNombre();
            String apellido = p.getUsuario().getApellido();
            if (apellido != null && !apellido.isBlank()) {
                nombreUsuario += " " + apellido;
            }
        }

        return ReporteResponseDTO.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .descripcion(p.getDescripcion())
                .categoria(p.getCategoria())
                .estado(p.getEstado())
                .fechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().format(ISO_FMT) : null)
                .latitud(p.getUbicacion().getY())
                .longitud(p.getUbicacion().getX())
                .emailUsuario(p.getUsuario() != null ? p.getUsuario().getEmail() : null)
                .nombreUsuario(nombreUsuario)
                .build();
    }
}
