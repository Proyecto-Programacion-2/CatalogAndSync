package com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Copia local de un horario semanal. {@code day_of_week} es el enum PostgreSQL
 * {@code catalog.day_of_week}; {@code NAMED_ENUM} hace que Hibernate lo bindee
 * como ese tipo y no como varchar.
 */
@Entity
@Table(name = "weekly_schedule", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class WeeklyScheduleEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private long professionalId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
