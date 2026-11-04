package com.programacion2.catalogandsync.sync.infrastructure.retry;

import com.programacion2.catalogandsync.sync.application.exception.SnapshotFetchException;
import com.programacion2.catalogandsync.sync.application.exception.SnapshotMappingException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryingSnapshotSourceAdapterTest {

    private static final Snapshot SNAPSHOT = new Snapshot(7L, List.of(), List.of(), List.of());

    @Test
    void retriesTransportFailuresWithNullStatus() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotSourcePort delegate = () -> {
            if (calls.incrementAndGet() == 1) {
                throw new SnapshotFetchException("timeout", new RuntimeException("timeout"), null, null);
            }
            return SNAPSHOT;
        };

        Snapshot result = adapter(delegate).fetchSnapshot();

        assertSame(SNAPSHOT, result);
        assertEquals(2, calls.get());
    }

    @Test
    void retriesServerErrors() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotSourcePort delegate = () -> {
            if (calls.incrementAndGet() == 1) {
                throw new SnapshotFetchException("unavailable", new RuntimeException("503"), 503, null);
            }
            return SNAPSHOT;
        };

        assertSame(SNAPSHOT, adapter(delegate).fetchSnapshot());
        assertEquals(2, calls.get());
    }

    @Test
    void doesNotRetryClientErrors() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotSourcePort delegate = () -> {
            calls.incrementAndGet();
            throw new SnapshotFetchException("not found", new RuntimeException("404"), 404, "NOT_FOUND");
        };

        SnapshotFetchException exception = assertThrows(
                SnapshotFetchException.class, () -> adapter(delegate).fetchSnapshot());

        assertEquals(404, exception.status().intValue());
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotRetryContractFailures() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotSourcePort delegate = () -> {
            calls.incrementAndGet();
            throw new SnapshotMappingException("missing categories");
        };

        assertThrows(SnapshotMappingException.class, () -> adapter(delegate).fetchSnapshot());
        assertEquals(1, calls.get());
    }

    @Test
    void givesUpAfterTheConfiguredAttempts() {
        AtomicInteger calls = new AtomicInteger();
        SnapshotSourcePort delegate = () -> {
            calls.incrementAndGet();
            throw new SnapshotFetchException("unavailable", new RuntimeException("503"), 503, null);
        };

        assertThrows(SnapshotFetchException.class, () -> adapter(delegate).fetchSnapshot());
        assertEquals(3, calls.get());
    }

    private static RetryingSnapshotSourceAdapter adapter(SnapshotSourcePort delegate) {
        return new RetryingSnapshotSourceAdapter(delegate, new FixedRetryExecutor(3, Duration.ZERO));
    }
}
