package _0.motovias_backend.service;

import _0.motovias_backend.dto.PuntoInteresRequestDTO;
import _0.motovias_backend.dto.PuntoInteresResponseDTO;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PuntoInteresService {

    private final PuntoInteresRepository repository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<PuntoInteresResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public PuntoInteresResponseDTO findById(Long id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de interés no encontrado"));
    }

    public List<PuntoInteresResponseDTO> buscarCercanos(double lat, double lon, double radioMetros) {
        return repository.findCercanos(lat, lon, radioMetros).stream()
                .map(this::toDTO)
                .toList();
    }

    public PuntoInteresResponseDTO crear(PuntoInteresRequestDTO dto, User usuario) {
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

    private PuntoInteresResponseDTO toDTO(PuntoInteres p) {
        String nombreUsuario = null;
        if (p.getUsuario() != null) {
            nombreUsuario = p.getUsuario().getNombre();
            String apellido = p.getUsuario().getApellido();
            if (apellido != null && !apellido.isBlank()) {
                nombreUsuario += " " + apellido;
            }
        }

        return PuntoInteresResponseDTO.builder()
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
