package _0.motovias_backend.service;

import _0.motovias_backend.dto.AdminReporteResponseDTO;
import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteEvento;
import _0.motovias_backend.model.TipoEventoReporte;
import _0.motovias_backend.model.TipoVoto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteEventoRepository;
import _0.motovias_backend.repository.ReporteVotoRepository;
import _0.motovias_backend.specification.PuntoInteresSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AdminReporteService {

    private final PuntoInteresRepository repository;
    private final ReporteEventoRepository eventoRepository;
    private final ReporteVotoRepository votoRepository;

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public Page<AdminReporteResponseDTO> listar(
            EstadoPunto estado,
            Categoria categoria,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable
    ) {
        return repository
                .findAll(PuntoInteresSpecification.conFiltros(estado, categoria, fechaInicio, fechaFin), pageable)
                .map(this::toDTO);
    }

    @Transactional
    public AdminReporteResponseDTO cambiarEstado(Long id, EstadoPunto nuevoEstado, User admin) {
        PuntoInteres punto = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        EstadoPunto estadoAnterior = punto.getEstado();
        punto.setEstado(nuevoEstado);
        PuntoInteres guardado = repository.save(punto);

        eventoRepository.save(ReporteEvento.builder()
                .reporte(guardado)
                .usuario(admin)
                .tipoEvento(TipoEventoReporte.CAMBIO_ESTADO)
                .descripcion("Estado actualizado de " + estadoAnterior + " a " + nuevoEstado
                        + " por el administrador " + admin.getEmail())
                .build());

        return toDTO(guardado);
    }

    private AdminReporteResponseDTO toDTO(PuntoInteres p) {
        String nombreUsuario = null;
        if (p.getUsuario() != null) {
            nombreUsuario = p.getUsuario().getNombre();
            String apellido = p.getUsuario().getApellido();
            if (apellido != null && !apellido.isBlank()) {
                nombreUsuario += " " + apellido;
            }
        }

        long confirmaciones = votoRepository.countByReporteAndTipoVoto(p, TipoVoto.CONFIRMA);
        long refutaciones = votoRepository.countByReporteAndTipoVoto(p, TipoVoto.REFUTA);

        return AdminReporteResponseDTO.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .categoria(p.getCategoria())
                .estado(p.getEstado())
                .nombreUsuarioCreador(nombreUsuario)
                .emailUsuarioCreador(p.getUsuario() != null ? p.getUsuario().getEmail() : null)
                .fechaCreacion(formatFechaArgentina(p.getFechaCreacion()))
                .votos(confirmaciones - refutaciones)
                .build();
    }

    // Ver comentario equivalente en ReporteService: el LocalDateTime ya viene asignado en
    // hora de pared de Argentina (ver @PrePersist en PuntoInteres) y acá solo se expone con
    // offset ISO-8601 explícito, sin alterar el valor.
    private String formatFechaArgentina(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZONA_ARGENTINA).format(ISO_FMT) : null;
    }
}
