package com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.IncrementalChangesResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Valida el contrato de {@code catedra:sync:changes:{version}} y extrae los IDs
 * afectados (IR §14.5).
 * <p>
 * Es puro y estatico: se testea sin Redis. Rechaza una version distinta de la
 * pedida, un {@code changes} ausente, una coleccion {@code null} y cualquier ID
 * nulo o no positivo. Un array vacio es valido: la version no cambia entidades.
 * <p>
 * Los IDs repetidos dentro de una misma coleccion se colapsan preservando el
 * orden de primera aparicion. No se rechazan: un duplicado no es ambiguo (mismo
 * ID, misma fila), pero hidratarlo dos veces haria dos {@code HGET} y permitiria
 * leer dos estados distintos de la misma entidad dentro de la misma version.
 */
@Slf4j
public final class IncrementalChangeMapper {

    private IncrementalChangeMapper() {
    }

    public static IncrementalChangeIds toIds(IncrementalChangesResponse response, long expectedVersion) {
        if (response == null) {
            throw new CatedraContractException("Catedra incremental changes are empty");
        }

        Long version = response.version();
        if (version == null) {
            throw new CatedraContractException("Catedra incremental changes version is missing");
        }
        if (version != expectedVersion) {
            throw new CatedraContractException("Catedra incremental changes version is " + version
                    + " but requested version was " + expectedVersion);
        }

        IncrementalChangesResponse.Changes changes = response.changes();
        if (changes == null) {
            throw new CatedraContractException(
                    "Catedra incremental changes for version " + version + " are missing the 'changes' object");
        }

        return new IncrementalChangeIds(
                requiredIds(changes.professionalCategories(), "professionalCategories", expectedVersion),
                requiredIds(changes.professionals(), "professionals", expectedVersion),
                requiredIds(changes.weeklySchedules(), "weeklySchedules", expectedVersion)
        );
    }

    private static List<Long> requiredIds(List<Long> ids, String field, long version) {
        if (ids == null) {
            throw new CatedraContractException("Catedra incremental changes " + version
                    + " is missing the required collection '" + field + "'");
        }

        LinkedHashSet<Long> unique = new LinkedHashSet<>(ids.size());
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new CatedraContractException("Catedra incremental changes " + version
                        + " has an invalid id in '" + field + "': " + id);
            }
            unique.add(id);
        }

        if (unique.size() != ids.size()) {
            log.debug("Catedra incremental changes {} had {} duplicate id(s) in '{}'; they were collapsed",
                    version, ids.size() - unique.size(), field);
        }
        return List.copyOf(unique);
    }

    /**
     * IDs afectados por una version, antes de hidratarlos desde los hashes.
     */
    public record IncrementalChangeIds(
            List<Long> categoryIds,
            List<Long> professionalIds,
            List<Long> scheduleIds
    ) {
    }
}
