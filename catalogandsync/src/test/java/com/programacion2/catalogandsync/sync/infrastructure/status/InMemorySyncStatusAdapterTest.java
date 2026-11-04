package com.programacion2.catalogandsync.sync.infrastructure.status;

import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.infrastructure.config.SyncProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySyncStatusAdapterTest {

    @Test
    void neverSyncedStatusIsEmptyAndStale() {
        SyncStatus status = adapter(60_000).currentStatus();

        assertNull(status.localVersion());
        assertNull(status.currentVersion());
        assertNull(status.oldestAvailableVersion());
        assertNull(status.lastSyncMode());
        assertNull(status.lastSyncAt());
        assertTrue(status.stale());
    }

    @Test
    void recordsTheLastSuccessfulCycle() {
        InMemorySyncStatusAdapter adapter = adapter(60_000);

        adapter.record(SyncMode.SNAPSHOT, 10L, 20L, 5L);

        SyncStatus status = adapter.currentStatus();
        assertEquals(10L, status.localVersion().longValue());
        assertEquals(20L, status.currentVersion().longValue());
        assertEquals(5L, status.oldestAvailableVersion().longValue());
        assertEquals(SyncMode.SNAPSHOT, status.lastSyncMode());
        assertNotNull(status.lastSyncAt());
        assertFalse(status.stale());
    }

    @Test
    void aCycleOlderThanTheThresholdIsStale() {
        InMemorySyncStatusAdapter adapter = adapter(0);

        adapter.record(SyncMode.NOTHING, 10L, 10L, 10L);

        assertTrue(adapter.currentStatus().stale());
    }

    @Test
    void theLatestCycleReplacesThePreviousOne() {
        InMemorySyncStatusAdapter adapter = adapter(60_000);

        adapter.record(SyncMode.NOTHING, 10L, 10L, 10L);
        adapter.record(SyncMode.SNAPSHOT, 11L, 11L, 10L);

        SyncStatus status = adapter.currentStatus();
        assertEquals(SyncMode.SNAPSHOT, status.lastSyncMode());
        assertEquals(11L, status.localVersion().longValue());
    }

    private static InMemorySyncStatusAdapter adapter(long staleAfterMs) {
        return new InMemorySyncStatusAdapter(new SyncProperties(
                null, null, null, new SyncProperties.Status(staleAfterMs)));
    }
}
