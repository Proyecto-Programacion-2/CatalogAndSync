package com.programacion2.catalogandsync.sync.domain.model;

import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;

import java.util.List;

/**
 * Una version incremental ya hidratada: las entidades completas que la catedra
 * afecto en esa version, no solo sus IDs.
 * <p>
 * El contrato de {@code changes:{version}} (IR §14.5) solo publica IDs; el
 * adaptador los resuelve contra los hashes de entidades. Tener el lote completo
 * permite aplicar la version en una sola transaccion de escritura.
 */
public record IncrementalBatch(
        long version,
        List<ProfessionalCategory> categories,
        List<Professional> professionals,
        List<WeeklySchedule> schedules
) {
}
