package com.programacion2.catalogandsync.sync.infrastructure.web.dto;

import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;

/**
 * Contrato de {@code GET /api/catalog/sync-status}. Los campos desconocidos se
 * serializan como {@code null}; {@code lastSyncAt} es ISO-8601 UTC y
 * {@code stale} lo calcula el almacen de estado.
 */
public record SyncStatusResponse(
        Long localVersion,
        Long currentVersion,
        Long oldestAvailableVersion,
        SyncMode lastSyncMode,
        String lastSyncAt,
        boolean stale
) {

    public static SyncStatusResponse from(SyncStatus status) {
        return new SyncStatusResponse(
                status.localVersion(),
                status.currentVersion(),
                status.oldestAvailableVersion(),
                status.lastSyncMode(),
                status.lastSyncAt() == null ? null : status.lastSyncAt().toString(),
                status.stale());
    }
}
