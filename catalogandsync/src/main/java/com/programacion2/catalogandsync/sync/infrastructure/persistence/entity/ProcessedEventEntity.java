package com.programacion2.catalogandsync.sync.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Evento Kafka ya procesado. La PK es el {@code eventId} de la catedra, no hay
 * secuencia local. La insercion real la hace el adaptador con
 * {@code INSERT ... ON CONFLICT DO NOTHING}; esta entidad existe para
 * {@code find} por PK y para que {@code ddl-auto=validate} cubra la tabla.
 */
@Entity
@Table(name = "processed_event", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 128)
    private String eventId;

    @Column(name = "applied_version", nullable = false)
    private long appliedVersion;

    @Column(name = "processed_at", nullable = false, insertable = false, updatable = false)
    private Instant processedAt;
}
