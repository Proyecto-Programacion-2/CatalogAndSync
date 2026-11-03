package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orquesta un snapshot: obtiene la copia de la catedra y la persiste de forma
 * atomica junto con la version. No conoce HTTP ni JPA (esos son los adaptadores).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotExecutor {

    private final SnapshotSourcePort snapshotSourcePort;
    private final CatalogWritePort catalogWritePort;

    public SyncOutcome execute(SyncDecision.TakeSnapshot decision) {
        log.info("Starting snapshot sync. Reason: {}", decision.reason());

        Snapshot snapshot = snapshotSourcePort.fetchSnapshot();

        catalogWritePort.applyVersion(
                snapshot.version(),
                SyncMode.SNAPSHOT,
                snapshot.categories(),
                snapshot.professionals(),
                snapshot.schedules()
        );

        log.info("Snapshot applied. Version: {} (categories={}, professionals={}, schedules={})",
                snapshot.version(),
                snapshot.categories().size(),
                snapshot.professionals().size(),
                snapshot.schedules().size());

        return new SyncOutcome(SyncMode.SNAPSHOT, snapshot.version(), false);
    }
}
