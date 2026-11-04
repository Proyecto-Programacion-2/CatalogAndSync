package com.programacion2.catalogandsync.sync.domain.ports.in;

import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;

/**
 * Consulta de solo lectura del estado de sincronizacion, expuesta por
 * {@code GET /api/catalog/sync-status}.
 */
public interface GetSyncStatusUseCase {

    SyncStatus status();
}
