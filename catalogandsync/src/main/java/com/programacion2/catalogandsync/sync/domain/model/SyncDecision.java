package com.programacion2.catalogandsync.sync.domain.model;


public sealed interface SyncDecision {

    record Nothing(long version) implements SyncDecision {

    }
    record Incremental(long fromVersion, long toVersion) implements SyncDecision {

    }

    record Snapshot(String reason) implements SyncDecision {
    }
}
