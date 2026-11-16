package com.programacion2.catalogandsync.sync.infrastructure.kafka.dto;

import java.time.Instant;

/**
 * Evento {@code CatalogUpdated} tal como viaja por Kafka (IR seccion 15.3).
 * <p>
 * Los campos son {@code null}ables a proposito: la ausencia de un obligatorio
 * es un contrato roto que el mapper debe reportar con contexto, no un valor por
 * defecto. Los campos desconocidos se ignoran (IR seccion 15.1: los
 * consumidores deben tolerar campos opcionales nuevos).
 */
public record CatalogUpdatedEvent(
        String eventId,
        String eventType,
        Long newVersion,
        Instant occurredAt,
        String source,
        Integer schemaVersion
) {
}
