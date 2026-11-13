package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;

/**
 * Profesional del catalogo. El ID y el de su categoria son los de la catedra.
 * <p>
 * Los invariantes viven en el dominio porque son ciertos con independencia de
 * la fuente (REST, Redis o cualquier camino futuro). El adaptador de entrada
 * sigue siendo responsable de la forma del cable y de traducir estas fallas a
 * la excepcion de su canal.
 */
public record Professional(
        long id,
        long categoryId,
        String firstName,
        String lastName,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public Professional {
        if (id <= 0) {
            throw new IllegalArgumentException("Professional id must be positive, got " + id);
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Professional categoryId must be positive, got " + categoryId);
        }
    }
}
