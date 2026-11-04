package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.exception.VersionSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.SyncStatusPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SyncServiceTest {

    private static final SyncOutcome OUTCOME = new SyncOutcome(SyncMode.SNAPSHOT, 10L, false);

    @Test
    void recordsTheSuccessfulCycleAndTheRemoteVersions() {
        VersionLedgerPort ledger = mock(VersionLedgerPort.class);
        when(ledger.currentVersion()).thenReturn(20L);
        when(ledger.oldestAvailableVersion()).thenReturn(5L);
        SyncStatusPort statusPort = mock(SyncStatusPort.class);

        SyncOutcome outcome = new SyncService(() -> OUTCOME, ledger, statusPort).sync();

        assertSame(OUTCOME, outcome);
        verify(statusPort).record(SyncMode.SNAPSHOT, 10L, 20L, 5L);
    }

    @Test
    void recordsNullRemoteVersionsWhenRedisFailsAfterTheCycle() {
        VersionLedgerPort ledger = mock(VersionLedgerPort.class);
        when(ledger.currentVersion())
                .thenThrow(new VersionSourceUnavailableException("redis down", new RuntimeException()));
        SyncStatusPort statusPort = mock(SyncStatusPort.class);

        new SyncService(() -> OUTCOME, ledger, statusPort).sync();

        verify(statusPort).record(SyncMode.SNAPSHOT, 10L, null, null);
    }

    @Test
    void doesNotRecordAFailedCycleAndReleasesTheLock() {
        AtomicInteger calls = new AtomicInteger();
        SyncCatalogUseCase useCase = () -> {
            if (calls.incrementAndGet() == 1) {
                throw new SyncException("snapshot failed");
            }
            return OUTCOME;
        };
        SyncStatusPort statusPort = mock(SyncStatusPort.class);
        SyncService service = new SyncService(useCase, stubLedger(), statusPort);

        assertThrows(SyncException.class, service::sync);

        verifyNoInteractions(statusPort);
        assertSame(OUTCOME, service.sync());
    }

    @Test
    void aSecondCycleIsRejectedWhileTheFirstOneIsRunning() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SyncCatalogUseCase useCase = () -> {
            started.countDown();
            await(release);
            return OUTCOME;
        };
        SyncService service = new SyncService(useCase, stubLedger(), mock(SyncStatusPort.class));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<SyncOutcome> first = executor.submit(service::sync);
            assertTrue(started.await(2, TimeUnit.SECONDS));

            assertThrows(SyncAlreadyRunningException.class, service::sync);

            release.countDown();
            assertSame(OUTCOME, first.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void statusDelegatesToTheStatusPort() {
        SyncStatus expected = new SyncStatus(10L, 20L, 5L, SyncMode.SNAPSHOT, Instant.now(), false);
        SyncStatusPort statusPort = mock(SyncStatusPort.class);
        when(statusPort.currentStatus()).thenReturn(expected);

        assertSame(expected, new SyncService(() -> OUTCOME, stubLedger(), statusPort).status());
    }

    private static VersionLedgerPort stubLedger() {
        return new VersionLedgerPort() {
            @Override
            public Optional<Long> localVersion() {
                return Optional.of(10L);
            }

            @Override
            public long currentVersion() {
                return 20L;
            }

            @Override
            public long oldestAvailableVersion() {
                return 5L;
            }
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
