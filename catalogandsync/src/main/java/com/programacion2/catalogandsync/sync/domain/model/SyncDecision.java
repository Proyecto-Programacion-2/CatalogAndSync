package com.programacion2.catalogandsync.sync.domain.model;


/**
 * Instructions produced by {@code SyncPlanner} for the sync orchestrator.
 * Each variant is an imperative command, not a data payload.
 */
public sealed interface SyncDecision {

    record DoNothing(long localVersion) implements SyncDecision {

    }
    record ApplyIncremental(long fromVersion, long toVersion) implements SyncDecision {

    }

    record TakeSnapshot(String reason) implements SyncDecision {
    }
}
