package com.programacion2.catalogandsync.sync.infrastructure.kafka.mapper;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.domain.model.CatalogUpdatedNotification;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test puro del contrato {@code CatalogUpdated} (IR seccion 15.3). No levanta
 * Kafka: el consumo real se verifica manualmente contra el broker de la
 * catedra.
 */
class CatalogUpdatedEventMapperTest {

    private static final String CONTEXT = "CatalogUpdated partition=0 offset=209";
    private static final String VALID_KEY = "211";

    private final CatalogUpdatedEventMapper mapper = new CatalogUpdatedEventMapper(
            JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build());

    @Test
    void mapsAValidEvent() {
        CatalogUpdatedNotification notification = mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"source\":\"SCHEDULED\""), CONTEXT);

        assertEquals("catalog-event-211-4f3b", notification.eventId());
        assertEquals(211, notification.newVersion());
        assertEquals(Instant.parse("2026-11-13T03:19:41.727773563Z"), notification.occurredAt());
        assertEquals("SCHEDULED", notification.source());
    }

    @Test
    void ignoresUnknownFieldsAndNullSource() {
        CatalogUpdatedNotification notification = mapper.toNotification(
                VALID_KEY, event(VALID_KEY, "\"source\":null,\"newField\":{\"nested\":true}"), CONTEXT);

        assertEquals(211, notification.newVersion());
        assertNull(notification.source());
    }

    @Test
    void rejectsEmptyPayload() {
        assertThrows(CatedraContractException.class, () -> mapper.toNotification(VALID_KEY, null, CONTEXT));
        assertThrows(CatedraContractException.class, () -> mapper.toNotification(VALID_KEY, " ", CONTEXT));
    }

    @Test
    void rejectsInvalidJson() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, "not-json", CONTEXT));

        assertTrue(ex.getMessage().contains("not valid JSON"));
    }

    @Test
    void rejectsWrongEventType() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"eventType\":\"OtherEvent\""), CONTEXT));

        assertTrue(ex.getMessage().contains("eventType"));
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"schemaVersion\":2"), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"schemaVersion\":null"), CONTEXT));
    }

    @Test
    void rejectsMissingEventId() {
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"eventId\":null"), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"eventId\":\"  \""), CONTEXT));
    }

    @Test
    void rejectsEventIdLongerThanTheColumn() {
        String tooLong = "e".repeat(129);
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"eventId\":\"" + tooLong + "\""), CONTEXT));

        assertTrue(ex.getMessage().contains("128"));
    }

    @Test
    void rejectsInvalidNewVersion() {
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"newVersion\":null"), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"newVersion\":0"), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"newVersion\":-3"), CONTEXT));
    }

    @Test
    void rejectsMissingOccurredAt() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(VALID_KEY, event(VALID_KEY, "\"occurredAt\":null"), CONTEXT));

        assertTrue(ex.getMessage().contains("occurredAt"));
    }

    @Test
    void requiresTheMessageKeyToBeTheVersion() {
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification(null, event(VALID_KEY, ""), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> mapper.toNotification("not-a-number", event(VALID_KEY, ""), CONTEXT));

        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> mapper.toNotification("210", event(VALID_KEY, ""), CONTEXT));
        assertTrue(ex.getMessage().contains("210"));
        assertTrue(ex.getMessage().contains("211"));
    }

    /**
     * Arma el evento con el {@code overrides} al final, para que el campo
     * repetido gane (Jackson toma la ultima aparicion).
     */
    private static String event(String key, String overrides) {
        String extra = overrides.isBlank() ? "" : "," + overrides;
        return """
                {
                  "eventId": "catalog-event-211-4f3b",
                  "eventType": "CatalogUpdated",
                  "newVersion": %s,
                  "occurredAt": "2026-11-13T03:19:41.727773563Z",
                  "source": "SCHEDULED",
                  "schemaVersion": 1
                  %s
                }
                """.formatted(key, extra);
    }
}
