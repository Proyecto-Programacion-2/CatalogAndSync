package com.programacion2.catalogandsync.sync.infrastructure.kafka.listener;

import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.service.SyncService;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.ProcessedEventPort;
import com.programacion2.catalogandsync.sync.infrastructure.kafka.mapper.CatalogUpdatedEventMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba la politica de R1 sin broker: el evento, el dedup, el disparo y el ack.
 * Los dobles son de nuestras capas ({@link SyncService},
 * {@link ProcessedEventPort}); no se emula Kafka ni la catedra.
 */
class CatalogUpdatedListenerTest {

    private static final String EVENT_ID = "catalog-event-211-4f3b";
    private static final String KEY = "211";

    private final CatalogUpdatedEventMapper mapper = new CatalogUpdatedEventMapper(
            JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build());
    private final ProcessedEventPort processedEventPort = mock(ProcessedEventPort.class);
    private final SyncService syncService = mock(SyncService.class);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
    private final CatalogUpdatedListener listener =
            new CatalogUpdatedListener(mapper, processedEventPort, syncService);

    @Test
    void appliesAnEventAndRecordsItAfterTheCycle() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(false);
        when(syncService.sync()).thenReturn(new SyncOutcome(SyncMode.INCREMENTAL, 211, false));

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verify(syncService).sync();
        verify(processedEventPort).record(EVENT_ID, 211);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void recordsTheActuallyAppliedVersionNotTheEventVersion() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(false);
        when(syncService.sync()).thenReturn(new SyncOutcome(SyncMode.INCREMENTAL, 215, false));

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verify(processedEventPort).record(EVENT_ID, 215);
    }

    @Test
    void skipsADuplicateWithoutSyncing() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(true);

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verifyNoInteractions(syncService);
        verify(processedEventPort, never()).record(any(), anyLong());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void discardsAContractViolationWithoutTouchingTheDedupTable() {
        listener.onCatalogUpdated(record(KEY, "not-json"), acknowledgment);

        verifyNoInteractions(syncService);
        verifyNoInteractions(processedEventPort);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void discardsAnEventWhoseKeyDoesNotMatchTheVersion() {
        listener.onCatalogUpdated(record("210", payload()), acknowledgment);

        verifyNoInteractions(syncService);
        verifyNoInteractions(processedEventPort);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void acksWhenTheCycleFailsBecauseTheReconciliationWillRetry() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(false);
        when(syncService.sync()).thenThrow(new SyncException("catedra unavailable"));

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verify(processedEventPort, never()).record(any(), anyLong());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void acksWhenAnotherCycleIsAlreadyRunning() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(false);
        when(syncService.sync()).thenThrow(new SyncAlreadyRunningException("A sync cycle is already running"));

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verify(processedEventPort, never()).record(any(), anyLong());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void acksEvenIfTheEventCannotBeRecordedAfterASuccessfulCycle() {
        when(processedEventPort.exists(EVENT_ID)).thenReturn(false);
        when(syncService.sync()).thenReturn(new SyncOutcome(SyncMode.NOTHING, 211, true));
        doThrow(new IllegalStateException("database down"))
                .when(processedEventPort).record(EVENT_ID, 211);

        listener.onCatalogUpdated(record(KEY, payload()), acknowledgment);

        verify(syncService).sync();
        verify(acknowledgment).acknowledge();
    }

    private static ConsumerRecord<String, String> record(String key, String value) {
        return new ConsumerRecord<>("catedra.catalog.test", 0, 209L, key, value);
    }

    private static String payload() {
        return """
                {
                  "eventId": "catalog-event-211-4f3b",
                  "eventType": "CatalogUpdated",
                  "newVersion": 211,
                  "occurredAt": "2026-11-13T03:19:41.727773563Z",
                  "source": "SCHEDULED",
                  "schemaVersion": 1
                }
                """;
    }
}
