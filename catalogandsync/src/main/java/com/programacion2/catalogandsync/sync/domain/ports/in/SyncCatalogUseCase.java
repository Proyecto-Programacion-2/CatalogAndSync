package com.programacion2.catalogandsync.sync.domain.ports.in;

import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;

public interface SyncCatalogUseCase {

    SyncOutcome sync();
}
