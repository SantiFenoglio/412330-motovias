package _0.motovias_backend.repository.projection;

import _0.motovias_backend.model.Categoria;

public interface CategoriaConteoProjection {
    Categoria getCategoria();
    long getCantidad();
}
