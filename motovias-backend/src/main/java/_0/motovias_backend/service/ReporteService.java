package _0.motovias_backend.service;

import _0.motovias_backend.dto.ReporteEventoResponseDTO;
import _0.motovias_backend.dto.ReporteRequestDTO;
import _0.motovias_backend.dto.ReporteResponseDTO;
import _0.motovias_backend.dto.ReporteUpdateDTO;
import _0.motovias_backend.dto.VotoRequestDTO;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.FotoReporte;
import _0.motovias_backend.model.FuenteUbicacion;
import _0.motovias_backend.model.PuntoInteres;
import _0.motovias_backend.model.ReporteEvento;
import _0.motovias_backend.model.ReporteVoto;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.TipoEventoReporte;
import _0.motovias_backend.model.TipoVoto;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.FotoReporteRepository;
import _0.motovias_backend.repository.PuntoInteresRepository;
import _0.motovias_backend.repository.ReporteEventoRepository;
import _0.motovias_backend.repository.ReporteVotoRepository;
import _0.motovias_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PuntoInteresRepository repository;
    private final UserRepository userRepository;
    private final ReporteVotoRepository votoRepository;
    private final ReporteEventoRepository eventoRepository;
    private final NotificacionService notificacionService;
    private final FotoReporteRepository fotoReporteRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.base-url}")
    private String uploadBaseUrl;

    @Value("${app.upload.max-fotos:3}")
    private int maxFotos;

    @Value("${app.upload.max-size-bytes:5242880}")
    private long maxSizeBytes;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public List<ReporteResponseDTO> listarMios(User usuario) {
        return repository.findByUsuarioIdAndEstadoNotOrderByFechaCreacionDesc(usuario.getId(), EstadoPunto.ELIMINADO)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ReporteResponseDTO crear(ReporteRequestDTO dto, User usuario) {
        Point ubicacion = GF.createPoint(new Coordinate(dto.getLongitud(), dto.getLatitud()));

        FuenteUbicacion fuente = dto.getFuenteUbicacion() != null ? dto.getFuenteUbicacion() : FuenteUbicacion.GPS;

        PuntoInteres entidad = PuntoInteres.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .categoria(dto.getCategoria())
                .ubicacion(ubicacion)
                .fuenteUbicacion(fuente)
                .usuario(usuario)
                .build();

        PuntoInteres guardado = repository.save(entidad);

        registrarEvento(guardado, usuario, TipoEventoReporte.CREACION, "Reporte creado");

        return toDTO(guardado);
    }

    @Transactional
    public ReporteResponseDTO editar(Long id, ReporteUpdateDTO dto) {
        PuntoInteres punto = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        User autenticado = validarAutoria(punto);
        EstadoPunto estadoAnterior = punto.getEstado();

        punto.setDescripcion(dto.getDescripcion());
        punto.setEstado(dto.getEstado());

        PuntoInteres guardado = repository.save(punto);

        if (estadoAnterior != dto.getEstado()) {
            registrarEvento(guardado, autenticado, TipoEventoReporte.CAMBIO_ESTADO,
                    "Estado actualizado de " + estadoAnterior + " a " + dto.getEstado());
        } else {
            registrarEvento(guardado, autenticado, TipoEventoReporte.EDICION, "Descripción actualizada");
        }

        if (EstadoPunto.RESUELTO.equals(dto.getEstado())) {
            notificacionService.archivarPorReporte(guardado);
        }

        return toDTO(guardado);
    }

    // Baja lógica: se conserva la fila para no romper la integridad referencial con
    // reporte_votos ni perder el historial de reporte_eventos. El reporte deja de
    // renderizarse en el mapa porque las consultas públicas excluyen ELIMINADO.
    @Transactional
    public ReporteResponseDTO eliminar(Long id) {
        PuntoInteres punto = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        User autenticado = validarAutoria(punto);
        EstadoPunto estadoAnterior = punto.getEstado();

        punto.setEstado(EstadoPunto.ELIMINADO);
        PuntoInteres guardado = repository.save(punto);

        String actor = Role.ADMIN.equals(autenticado.getRole()) ? "el administrador" : "el propietario";
        registrarEvento(guardado, autenticado, TipoEventoReporte.CAMBIO_ESTADO,
                "Reporte dado de baja lógica (de " + estadoAnterior + " a ELIMINADO) por "
                        + actor + " " + autenticado.getEmail());

        return toDTO(guardado);
    }

    @Transactional
    public ReporteResponseDTO votar(Long reporteId, VotoRequestDTO dto, User usuario) {
        PuntoInteres punto = repository.findById(reporteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        if (punto.getUsuario() != null && punto.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No podés votar tu propio reporte");
        }

        Optional<ReporteVoto> votoExistente = votoRepository.findByUsuarioAndReporte(usuario, punto);

        String descripcionVoto;
        if (votoExistente.isPresent()) {
            ReporteVoto voto = votoExistente.get();
            if (voto.getTipoVoto() == dto.getTipoVoto()) {
                votoRepository.delete(voto);
                descripcionVoto = "Voto removido (" + dto.getTipoVoto() + ")";
            } else {
                voto.setTipoVoto(dto.getTipoVoto());
                votoRepository.save(voto);
                descripcionVoto = "Voto actualizado a " + dto.getTipoVoto();
            }
        } else {
            votoRepository.save(ReporteVoto.builder()
                    .usuario(usuario)
                    .reporte(punto)
                    .tipoVoto(dto.getTipoVoto())
                    .build());
            descripcionVoto = "Voto registrado: " + dto.getTipoVoto();
        }

        registrarEvento(punto, usuario, TipoEventoReporte.VOTO, descripcionVoto);

        long confirmaciones = votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.CONFIRMA);
        long refutaciones = votoRepository.countByReporteAndTipoVoto(punto, TipoVoto.REFUTA);
        long balance = confirmaciones - refutaciones;

        if (balance <= -5 && punto.getEstado() != EstadoPunto.DUDOSO) {
            EstadoPunto estadoAnterior = punto.getEstado();
            punto.setEstado(EstadoPunto.DUDOSO);
            repository.save(punto);
            registrarEvento(punto, null, TipoEventoReporte.CAMBIO_ESTADO,
                    "Estado actualizado de " + estadoAnterior + " a DUDOSO por validación comunitaria");
        }

        ReporteResponseDTO response = toDTO(punto);
        response.setConfirmaciones(confirmaciones);
        response.setRefutaciones(refutaciones);
        return response;
    }

    public List<ReporteEventoResponseDTO> obtenerHistorial(Long reporteId) {
        PuntoInteres reporte = repository.findById(reporteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        return eventoRepository.findByReporteOrderByTimestampDesc(reporte)
                .stream()
                .map(this::toEventoDTO)
                .toList();
    }

    @Transactional
    public ReporteResponseDTO subirFotos(Long reporteId, MultipartFile[] archivos) {
        PuntoInteres punto = repository.findById(reporteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        User autenticado = validarAutoria(punto);

        if (archivos == null || archivos.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe adjuntar al menos una foto");
        }

        long existentes = fotoReporteRepository.countByReporte(punto);
        if (existentes + archivos.length > maxFotos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El reporte no puede tener más de " + maxFotos + " fotos");
        }

        for (MultipartFile archivo : archivos) {
            validarArchivo(archivo);
        }

        for (MultipartFile archivo : archivos) {
            String nombreArchivo = guardarArchivo(archivo);
            fotoReporteRepository.save(FotoReporte.builder()
                    .reporte(punto)
                    .rutaArchivo(nombreArchivo)
                    .build());
        }

        registrarEvento(punto, autenticado, TipoEventoReporte.EDICION,
                "Se agregaron " + archivos.length + " foto(s) al reporte");

        return toDTO(punto);
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }
        if (archivo.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo permitido de 5MB");
        }

        String tipoContenido = archivo.getContentType();
        boolean tipoValido = MediaType.IMAGE_JPEG_VALUE.equals(tipoContenido)
                || MediaType.IMAGE_PNG_VALUE.equals(tipoContenido);
        if (!tipoValido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de archivo no soportado: solo se permiten imágenes JPEG o PNG");
        }
    }

    private String guardarArchivo(MultipartFile archivo) {
        try {
            Path directorio = Path.of(uploadDir);
            Files.createDirectories(directorio);

            String extension = MediaType.IMAGE_PNG_VALUE.equals(archivo.getContentType()) ? ".png" : ".jpg";
            String nombreArchivo = UUID.randomUUID() + extension;

            archivo.transferTo(directorio.resolve(nombreArchivo));
            return nombreArchivo;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto", e);
        }
    }

    private void registrarEvento(PuntoInteres reporte, User usuario, TipoEventoReporte tipo, String descripcion) {
        eventoRepository.save(ReporteEvento.builder()
                .reporte(reporte)
                .usuario(usuario)
                .tipoEvento(tipo)
                .descripcion(descripcion)
                .build());
    }

    private User validarAutoria(PuntoInteres punto) {
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        User autenticado = userRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean esDuenio = punto.getUsuario() != null
                && emailAutenticado.equals(punto.getUsuario().getEmail());
        boolean esAdmin = Role.ADMIN.equals(autenticado.getRole());

        if (!esDuenio && !esAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permiso para modificar este reporte");
        }

        return autenticado;
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
                .fechaCreacion(formatFechaArgentina(p.getFechaCreacion()))
                .latitud(p.getUbicacion().getY())
                .longitud(p.getUbicacion().getX())
                .emailUsuario(p.getUsuario() != null ? p.getUsuario().getEmail() : null)
                .nombreUsuario(nombreUsuario)
                .fuenteUbicacion(p.getFuenteUbicacion())
                .fotos(obtenerUrlsFotos(p))
                .build();
    }

    private List<String> obtenerUrlsFotos(PuntoInteres p) {
        return fotoReporteRepository.findByReporteOrderByFechaCargaAsc(p).stream()
                .map(f -> uploadBaseUrl + "/uploads/" + f.getRutaArchivo())
                .toList();
    }

    private ReporteEventoResponseDTO toEventoDTO(ReporteEvento evento) {
        String nombreUsuario = null;
        if (evento.getUsuario() != null) {
            nombreUsuario = evento.getUsuario().getNombre();
            String apellido = evento.getUsuario().getApellido();
            if (apellido != null && !apellido.isBlank()) {
                nombreUsuario += " " + apellido;
            }
        }

        return ReporteEventoResponseDTO.builder()
                .id(evento.getId())
                .tipoEvento(evento.getTipoEvento())
                .descripcion(evento.getDescripcion())
                .nombreUsuario(nombreUsuario)
                .timestamp(formatFechaArgentina(evento.getTimestamp()))
                .build();
    }

    // Los campos LocalDateTime (asignados en @PrePersist en PuntoInteres/ReporteEvento)
    // ya representan la hora de pared de Argentina en la que ocurrió el evento. Acá solo
    // se etiqueta ese valor con el offset ISO-8601 explícito (-03:00), sin alterarlo, para
    // que el frontend no dependa de la zona horaria del navegador ni del servidor al parsear
    // la fecha.
    private String formatFechaArgentina(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZONA_ARGENTINA).format(ISO_FMT) : null;
    }
}
