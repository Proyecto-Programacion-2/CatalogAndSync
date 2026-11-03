package com.programacion2.catalogandsync.sync.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta cruda de {@code GET /api/synchronization/snapshot} (IR §7).
 * Los campos se validan en el mapper; aca solo se refleja el contrato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SnapshotResponse(
        Long snapshotVersion,
        Instant generatedAt,
        List<ProfessionalCategoryResponse> professionalCategories,
        List<ProfessionalResponse> professionals,
        List<WeeklyScheduleResponse> weeklySchedules
) {
}
