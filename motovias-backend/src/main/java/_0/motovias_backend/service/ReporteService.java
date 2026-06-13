package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.dto.ReporteUpdateDTO;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PuntoInteresRepository repository;
    private final UserRepository userRepository;

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
