package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;

/**
 * Categoria del catalogo. El ID es el de la catedra: no hay secuencia local.
 * <p>
 * Los invariantes viven en el dominio porque son ciertos con independencia de
 * la fuente (REST, Redis o cualquier camino futuro). El adaptador de entrada
 * sigue siendo responsable de la forma del cable y de traducir estas fallas a
 * la excepcion de su canal.
 */
public record ProfessionalCategory(
        long id,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public ProfessionalCategory {
        if (id <= 0) {
            throw new IllegalArgumentException("ProfessionalCategory id must be positive, got " + id);
        }
    }
}
