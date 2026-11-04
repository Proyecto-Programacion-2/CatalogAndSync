package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.application.exception.SyncAlreadyRunningException;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.domain.ports.in.GetSyncStatusUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.SyncStatusPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Coordinador de los ciclos de sincronizacion (H1.9).
 * <p>
 * Responsabilidades:
 * <ul>
 *   <li>garantizar un solo ciclo a la vez con {@code tryLock}; si ya corre
 *       otro, lanza {@link SyncAlreadyRunningException} y el disparador lo
 *       saltea;</li>
 *   <li>registrar el resultado del ultimo ciclo exitoso para
 *       {@code GET /api/catalog/sync-status};</li>
 *   <li>delegar la decision y la escritura en {@link SyncCatalogUseCase}.</li>
 * </ul>
 * El lock es en memoria: vale para una sola instancia, que es el alcance
 * acordado. No se bloquea al segundo llamador para no acumular ciclos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService implements GetSyncStatusUseCase {

    private final SyncCatalogUseCase syncCatalogUseCase;
    private final VersionLedgerPort versionLedgerPort;
    private final SyncStatusPort syncStatusPort;
    private final ReentrantLock cycleLock = new ReentrantLock();

    public SyncOutcome sync() {
        if (!cycleLock.tryLock()) {
            throw new SyncAlreadyRunningException("A sync cycle is already running");
        }
        try {
            SyncOutcome outcome = syncCatalogUseCase.sync();
            recordSuccessfulCycle(outcome);
            return outcome;
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public SyncStatus status() {
        return syncStatusPort.currentStatus();
    }

    private void recordSuccessfulCycle(SyncOutcome outcome) {
        try {
            Long currentVersion = readRemoteVersion(
                    versionLedgerPort::currentVersion, "current-version");
            Long oldestAvailableVersion = currentVersion == null ? null : readRemoteVersion(
                    versionLedgerPort::oldestAvailableVersion, "oldest-available-version");
            syncStatusPort.record(
                    outcome.mode(), outcome.appliedVersion(), currentVersion, oldestAvailableVersion);
        } catch (RuntimeException ex) {
            // El ciclo ya se aplicó: un fallo al registrar estado no lo invalida.
            log.warn("Could not record sync status: {}", ex.getMessage());
        }
    }

    private Long readRemoteVersion(Supplier<Long> read, String name) {
        try {
            return read.get();
        } catch (RuntimeException ex) {
            log.warn("Could not read {} for sync status: {}", name, ex.getMessage());
            return null;
        }
    }
}
