package com.programacion2.catalogandsync.sync.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contenido crudo de {@code catedra:sync:changes:{version}} (IR §14.5).
 * <p>
 * Los arrays contienen IDs afectados, no entidades completas. El contrato se
 * valida en el mapper; aca solo se refleja.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncrementalChangesResponse(
        Long version,
        Changes changes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Changes(
            List<Long> professionalCategories,
            List<Long> professionals,
            List<Long> weeklySchedules
    ) {
    }
}
