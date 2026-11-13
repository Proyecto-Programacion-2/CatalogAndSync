package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Horario semanal de atencion de un profesional. El ID es el de la catedra.
 * <p>
 * Los invariantes viven en el dominio porque son ciertos con independencia de
 * la fuente (REST, Redis o cualquier camino futuro). El adaptador de entrada
 * sigue siendo responsable de la forma del cable y de traducir estas fallas a
 * la excepcion de su canal.
 */
public record WeeklySchedule(
        long id,
        long professionalId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public WeeklySchedule {
        if (id <= 0) {
            throw new IllegalArgumentException("WeeklySchedule id must be positive, got " + id);
        }
        if (professionalId <= 0) {
            throw new IllegalArgumentException(
                    "WeeklySchedule professionalId must be positive, got " + professionalId);
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("WeeklySchedule dayOfWeek is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("WeeklySchedule startTime is required");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("WeeklySchedule endTime is required");
        }
        // La catedra no documenta horarios que crucen la medianoche: una ventana
        // invertida o vacia es un dato invalido, no un turno nocturno.
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "WeeklySchedule startTime " + startTime + " must be before endTime " + endTime);
        }
        if (slotDurationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "WeeklySchedule slotDurationMinutes must be positive, got " + slotDurationMinutes);
        }
    }
}
