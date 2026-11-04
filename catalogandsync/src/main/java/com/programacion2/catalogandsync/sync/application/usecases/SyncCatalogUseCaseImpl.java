package com.programacion2.catalogandsync.sync.application.usecases;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogConsistencyPort;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.service.SyncPlanner;
import com.programacion2.catalogandsync.sync.application.service.executors.IncrementalExecutor;
import com.programacion2.catalogandsync.sync.application.service.executors.NothingExecutor;
import com.programacion2.catalogandsync.sync.application.service.executors.SnapshotExecutor;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SyncCatalogUseCaseImpl implements SyncCatalogUseCase {

    private final SyncPlanner syncPlanner;
    private final VersionLedgerPort versionLedgerPort;
    private final CatalogConsistencyPort catalogConsistencyPort;
    private final NothingExecutor nothingExecutor;
    private final IncrementalExecutor incrementalExecutor;
    private final SnapshotExecutor snapshotExecutor;

    @Override
    public SyncOutcome sync() {
        try {
            Optional<Long> localVersion = versionLedgerPort.localVersion();
            long currentVersion = versionLedgerPort.currentVersion();
            long oldestAvailableVersion = versionLedgerPort.oldestAvailableVersion();
            boolean localStateVerifiable = catalogConsistencyPort.isStateVerifiable(localVersion);

            SyncDecision decision = syncPlanner.decide(
                    localVersion, currentVersion, oldestAvailableVersion, localStateVerifiable);

            return switch (decision) {
                case SyncDecision.DoNothing doNothing -> nothingExecutor.execute(doNothing);
                case SyncDecision.TakeSnapshot takeSnapshot -> snapshotExecutor.execute(takeSnapshot);
                case SyncDecision.ApplyIncremental applyIncremental -> incrementalExecutor.execute(applyIncremental);
            };
        } catch (SyncException e) {
            throw e;
        } catch (Exception e) {
            throw new SyncException("Error in the sync process", e);
        }
    }
}
