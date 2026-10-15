package com.programacion2.catalogandsync.sync.domain.model;

public record SyncOutcome(
        SyncMode mode,
        long appliedVersion,
        boolean nothingChanged
) {
}
