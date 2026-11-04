package com.programacion2.catalogandsync.sync.infrastructure.scheduling;

import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.service.SyncService;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.SyncStatusPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * El scheduler no debe morir ante un ciclo fallido ni ante uno salteado: la
 * recuperacion la hace el proximo intento (reconciliacion). Se prueba con
 * dobles de puertos propios.
 */
class SyncSchedulerTest {

    @Test
    void aSuccessfulCycleDoesNotThrow() {
        SyncScheduler scheduler = new SyncScheduler(
                syncService(() -> new SyncOutcome(SyncMode.NOTHING, 1L, true)));

        assertDoesNotThrow(scheduler::runCycle);
    }

    @Test
    void aFailedCycleIsSwallowedSoTheNextRunCanRecover() {
        SyncScheduler scheduler = new SyncScheduler(
                syncService(() -> {
                    throw new SyncException("catedra redis is down");
                }));

        assertDoesNotThrow(scheduler::runCycle);
    }

    @Test
    void aSkippedCycleIsSwallowed() {
        SyncScheduler scheduler = new SyncScheduler(
                syncService(() -> {
                    throw new SyncAlreadyRunningException("A sync cycle is already running");
                }));

        assertDoesNotThrow(scheduler::runCycle);
    }

    private static SyncService syncService(SyncCatalogUseCase useCase) {
        return new SyncService(useCase, new StubVersionLedger(), new NoOpSyncStatusPort());
    }

    private static final class StubVersionLedger implements VersionLedgerPort {
        @Override
        public Optional<Long> localVersion() {
            return Optional.of(1L);
        }

        @Override
        public long currentVersion() {
            return 1L;
        }

        @Override
        public long oldestAvailableVersion() {
            return 1L;
        }
    }

    private static final class NoOpSyncStatusPort implements SyncStatusPort {
        @Override
        public void record(SyncMode mode, long localVersion, Long currentVersion, Long oldestAvailableVersion) {
        }

        @Override
        public SyncStatus currentStatus() {
            return new SyncStatus(null, null, null, null, null, true);
        }
    }
}
