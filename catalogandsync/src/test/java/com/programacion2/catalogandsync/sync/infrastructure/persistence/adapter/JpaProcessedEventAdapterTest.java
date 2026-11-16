package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dedup de eventos Kafka sobre {@code catalog.processed_event} (V2). El insert
 * usa {@code ON CONFLICT DO NOTHING}: repetir el mismo {@code eventId} no lanza
 * ni pisa el primer registro.
 * <p>
 * {@code @Transactional} revierte todo lo insertado al terminar el test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaProcessedEventAdapterTest {

    private static final String EVENT_ID = "catalog-event-211-4f3b";

    @Autowired
    private JpaProcessedEventAdapter adapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsNothingForAnUnknownEvent() {
        assertFalse(adapter.exists(EVENT_ID));
    }

    @Test
    void recordsAndFindsAnEvent() {
        adapter.record(EVENT_ID, 211);

        assertTrue(adapter.exists(EVENT_ID));
        assertEquals(211, storedVersion(EVENT_ID));
        assertNotNull(storedProcessedAt(EVENT_ID));
    }

    @Test
    void keepsTheFirstRecordWhenTheSameEventArrivesAgain() {
        adapter.record(EVENT_ID, 211);
        adapter.record(EVENT_ID, 215);

        assertTrue(adapter.exists(EVENT_ID));
        assertEquals(211, storedVersion(EVENT_ID));
    }

    @Test
    void distinguishesDifferentEvents() {
        adapter.record(EVENT_ID, 211);

        assertFalse(adapter.exists("catalog-event-212-4f3b"));
    }

    @Test
    void acceptsTheMaximumEventIdLength() {
        String eventId = "e".repeat(128);

        adapter.record(eventId, 211);

        assertTrue(adapter.exists(eventId));
    }

    private long storedVersion(String eventId) {
        return ((Number) entityManager.createNativeQuery(
                        "SELECT applied_version FROM catalog.processed_event WHERE event_id = :eventId")
                .setParameter("eventId", eventId)
                .getSingleResult()).longValue();
    }

    private Object storedProcessedAt(String eventId) {
        return entityManager.createNativeQuery(
                        "SELECT processed_at FROM catalog.processed_event WHERE event_id = :eventId")
                .setParameter("eventId", eventId)
                .getSingleResult();
    }
}
