package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SyncPlannerTest {

    private final SyncPlanner planner = new SyncPlanner();

    @Test
    void emptyLocalDatabaseRequiresSnapshot() {
        SyncDecision decision = planner.decide(Optional.empty(), 7, 4, true);

        assertInstanceOf(SyncDecision.Snapshot.class, decision);
    }

    @Test
    void localVersionOlderThanWindowRequiresSnapshot() {
        SyncDecision decision = planner.decide(Optional.of(3L), 7, 4, true);

        assertInstanceOf(SyncDecision.Snapshot.class, decision);
    }

    @Test
    void localVersionAtCurrentVersionDoesNothing() {
        SyncDecision decision = planner.decide(Optional.of(7L), 7, 4, true);

        assertInstanceOf(SyncDecision.Nothing.class, decision);
    }

    @Test
    void localVersionAheadOfCurrentDoesNothing() {
        SyncDecision decision = planner.decide(Optional.of(8L), 7, 4, true);

        assertInstanceOf(SyncDecision.Nothing.class, decision);
    }

    @Test
    void localVersionBehindCurrentStartsIncrementalFromNextVersion() {
        SyncDecision decision = planner.decide(Optional.of(5L), 7, 4, true);

        assertEquals(new SyncDecision.Incremental(6L), decision);
    }

    @Test
    void unverifiableLocalStateRequiresSnapshot() {
        SyncDecision decision = planner.decide(Optional.of(7L), 7, 4, false);

        assertInstanceOf(SyncDecision.Snapshot.class, decision);
    }
}
