package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.sync.domain.ports.out.ProcessedEventPort;
import com.programacion2.catalogandsync.sync.infrastructure.persistence.entity.ProcessedEventEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistencia del dedup de eventos Kafka en {@code catalog.processed_event}.
 * <p>
 * {@link #record} usa {@code INSERT ... ON CONFLICT DO NOTHING}: si el evento
 * ya estaba, no lanza y no ensucia la transaccion. Es la unica escritura de la
 * tabla; la lectura previa ({@link #exists}) es la que evita repetir un ciclo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaProcessedEventAdapter implements ProcessedEventPort {

    private static final String INSERT_EVENT = """
            INSERT INTO catalog.processed_event (event_id, applied_version)
            VALUES (:eventId, :appliedVersion)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String eventId) {
        return entityManager.find(ProcessedEventEntity.class, eventId) != null;
    }

    @Override
    @Transactional
    public void record(String eventId, long appliedVersion) {
        int inserted = entityManager.createNativeQuery(INSERT_EVENT)
                .setParameter("eventId", eventId)
                .setParameter("appliedVersion", appliedVersion)
                .executeUpdate();

        if (inserted == 0) {
            log.debug("Catalog event {} was already recorded; keeping the first applied version", eventId);
        }
    }
}
