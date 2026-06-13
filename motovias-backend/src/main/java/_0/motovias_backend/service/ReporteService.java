package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PuntoInteresRepository repository;

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
