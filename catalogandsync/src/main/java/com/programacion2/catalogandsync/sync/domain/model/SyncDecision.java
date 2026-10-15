package com.programacion2.catalogandsync.sync.domain.model;

import java.util.Optional;

public sealed interface SyncDecision {

    record Nothing() implements SyncDecision {
    }

    record Incremental(long fromVersion) implements SyncDecision {
    }

    record Snapshot(String reason) implements SyncDecision {
    }
}
