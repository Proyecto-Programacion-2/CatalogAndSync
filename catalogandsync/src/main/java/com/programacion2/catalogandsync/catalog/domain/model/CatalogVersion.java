package com.programacion2.catalogandsync.catalog.domain.model;

import java.time.Instant;

public record CatalogVersion(
        long localVersion,
        String lastSyncMode,
        Instant updatedAt
) {
}
