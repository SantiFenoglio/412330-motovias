package _0.motovias_backend.service;

import _0.motovias_backend.dto.ComercioVerificadoRequestDTO;
import _0.motovias_backend.dto.ComercioVerificadoResponseDTO;
import _0.motovias_backend.model.ComercioVerificado;
import _0.motovias_backend.repository.ComercioVerificadoRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComercioVerificadoService {

    private final ComercioVerificadoRepository repository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public List<ComercioVerificadoResponseDTO> listarActivos() {
        return repository.findByActivoTrue().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ComercioVerificadoResponseDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ComercioVerificadoResponseDTO crear(ComercioVerificadoRequestDTO dto) {
        Point ubicacion = GF.createPoint(new Coordinate(dto.getLongitud(), dto.getLatitud()));

        ComercioVerificado entidad = ComercioVerificado.builder()
                .nombre(dto.getNombre())
                .direccion(dto.getDireccion())
                .telefono(dto.getTelefono())
                .categoria(dto.getCategoria())
                .ubicacion(ubicacion)
                .activo(true)
                .build();

        return toDTO(repository.save(entidad));
    }

    @Transactional
    public ComercioVerificadoResponseDTO editar(Long id, ComercioVerificadoRequestDTO dto) {
        ComercioVerificado comercio = buscarOFallar(id);

        comercio.setNombre(dto.getNombre());
        comercio.setDireccion(dto.getDireccion());
        comercio.setTelefono(dto.getTelefono());
        comercio.setCategoria(dto.getCategoria());
        comercio.setUbicacion(GF.createPoint(new Coordinate(dto.getLongitud(), dto.getLatitud())));

        return toDTO(repository.save(comercio));
    }

    @Transactional
    public ComercioVerificadoResponseDTO cambiarEstado(Long id, boolean activo) {
        ComercioVerificado comercio = buscarOFallar(id);
        comercio.setActivo(activo);
        return toDTO(repository.save(comercio));
    }

    private ComercioVerificado buscarOFallar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comercio no encontrado"));
    }

    private ComercioVerificadoResponseDTO toDTO(ComercioVerificado c) {
        return ComercioVerificadoResponseDTO.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .direccion(c.getDireccion())
                .telefono(c.getTelefono())
                .categoria(c.getCategoria())
                .latitud(c.getUbicacion().getY())
                .longitud(c.getUbicacion().getX())
                .activo(Boolean.TRUE.equals(c.getActivo()))
                .verificado(true)
                .fechaAlta(c.getFechaAlta() != null ? c.getFechaAlta().format(ISO_FMT) : null)
                .fechaModificacion(c.getFechaModificacion() != null ? c.getFechaModificacion().format(ISO_FMT) : null)
                .build();
    }
}
