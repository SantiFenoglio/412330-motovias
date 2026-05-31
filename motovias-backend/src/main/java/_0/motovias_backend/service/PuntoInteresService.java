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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PuntoInteresService {

    private final PuntoInteresRepository repository;

    // GeometryFactory es thread-safe; instancia única con SRID 4326 (WGS84)
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public List<PuntoInteresResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<PuntoInteresResponseDTO> buscarCercanos(double lat, double lon, double radioMetros) {
        return repository.findCercanos(lat, lon, radioMetros).stream()
                .map(this::toDTO)
                .toList();
    }

    public PuntoInteresResponseDTO crear(PuntoInteresRequestDTO dto, User usuario) {
        // JTS Coordinate: (x=longitud, y=latitud) — orden cartesiano, no geográfico
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
        return PuntoInteresResponseDTO.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .descripcion(p.getDescripcion())
                .categoria(p.getCategoria())
                // getY() = latitud, getX() = longitud (convención JTS)
                .latitud(p.getUbicacion().getY())
                .longitud(p.getUbicacion().getX())
                .emailUsuario(p.getUsuario() != null ? p.getUsuario().getEmail() : null)
                .build();
    }
}
