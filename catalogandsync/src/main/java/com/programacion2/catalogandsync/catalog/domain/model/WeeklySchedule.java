package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;
import java.time.LocalTime;

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
}
