package com.programacion2.catalogandsync.catalog.domain.ports.out;

import java.util.Optional;

/**
 * Verifica si el estado local del catalogo es confiable para decidir un
 * incremental (DISENO_SYNC §3).
 * <p>
 * Es fail-safe para errores de acceso a datos: si la verificacion no puede
 * completarse, devuelve {@code false}, que el planner interpreta como "tomar
 * snapshot". Los errores de programacion no se enmascaran.
 */
public interface CatalogConsistencyPort {
    boolean isStateVerifiable(Optional<Long> version);
}
