package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;

public class SnapshotSyncService implements SyncCatalogUseCase {

    private final SyncPlanner planner;
    private final VersionLedgerPort versionLedger;
    private final SnapshotSourcePort snapshotSource;
    private final CatalogWritePort catalogWrite;

    public SnapshotSyncService(
            SyncPlanner planner,
            VersionLedgerPort versionLedger,
            SnapshotSourcePort snapshotSource,
            CatalogWritePort catalogWrite
    ) {
        this.planner = planner;
        this.versionLedger = versionLedger;
        this.snapshotSource = snapshotSource;
        this.catalogWrite = catalogWrite;
    }

    @Override
    public SyncOutcome sync() {
        throw new UnsupportedOperationException();
    }
}
