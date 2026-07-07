package _0.motovias_backend.specification;

import _0.motovias_backend.model.Categoria;
import _0.motovias_backend.model.EstadoPunto;
import _0.motovias_backend.model.PuntoInteres;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtros dinámicos y combinables para la consulta paginada de reportes
 * del panel de administración. Cada filtro es opcional: si el valor
 * recibido es nulo, la Specification no agrega ninguna restricción.
 */
public final class PuntoInteresSpecification {

    private PuntoInteresSpecification() {
    }

    public static Specification<PuntoInteres> conFiltros(
            EstadoPunto estado,
            Categoria categoria,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    ) {
        return Specification
                .where(estado(estado))
                .and(categoria(categoria))
                .and(fechaDesde(fechaInicio))
                .and(fechaHasta(fechaFin));
    }

    private static Specification<PuntoInteres> estado(EstadoPunto estado) {
        return (root, query, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    private static Specification<PuntoInteres> categoria(Categoria categoria) {
        return (root, query, cb) -> categoria == null ? null : cb.equal(root.get("categoria"), categoria);
    }

    private static Specification<PuntoInteres> fechaDesde(LocalDateTime fechaInicio) {
        return (root, query, cb) -> fechaInicio == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaInicio);
    }

    private static Specification<PuntoInteres> fechaHasta(LocalDateTime fechaFin) {
        return (root, query, cb) -> fechaFin == null
                ? null
                : cb.lessThanOrEqualTo(root.get("fechaCreacion"), fechaFin);
    }
}
