package com.programacion2.catalogandsync.sync.domain.ports.out;

import com.programacion2.catalogandsync.sync.domain.model.Snapshot;

public interface SnapshotSourcePort {

    Snapshot fetchSnapshot();
}
