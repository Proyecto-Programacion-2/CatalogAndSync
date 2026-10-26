package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SyncPlannerTest {

    private final SyncPlanner planner = new SyncPlanner();

    // --- Snapshot cases -----------------------------------------------------

    @Test
    void unverifiableLocalStateTakesSnapshot() {
        SyncDecision decision = planner.decide(Optional.of(7L), 7, 4, false);

        assertInstanceOf(SyncDecision.TakeSnapshot.class, decision);
    }

    @Test
    void unverifiableStateWinsOverEmptyLocalCatalog() {
        // localStateVerifiable is checked before emptiness, so the diagnostic
        // reason must be the broken state, not "empty catalog".
        SyncDecision decision = planner.decide(Optional.empty(), 7, 4, false);

        assertInstanceOf(SyncDecision.TakeSnapshot.class, decision);
    }

    @Test
    void emptyLocalDatabaseTakesSnapshot() {
        SyncDecision decision = planner.decide(Optional.empty(), 7, 4, true);

        assertInstanceOf(SyncDecision.TakeSnapshot.class, decision);
    }

    @Test
    void localVersionOlderThanWindowTakesSnapshot() {
        SyncDecision decision = planner.decide(Optional.of(3L), 7, 4, true);

        assertInstanceOf(SyncDecision.TakeSnapshot.class, decision);
    }

    // --- Incremental cases --------------------------------------------------

    @Test
    void localVersionAtOldestWindowBoundaryAppliesIncremental() {
        SyncDecision decision = planner.decide(Optional.of(4L), 7, 4, true);

        assertEquals(new SyncDecision.ApplyIncremental(5L, 7L), decision);
    }

    @Test
    void localVersionBehindCurrentAppliesIncrementalFromNextVersion() {
        SyncDecision decision = planner.decide(Optional.of(5L), 7, 4, true);

        assertEquals(new SyncDecision.ApplyIncremental(6L, 7L), decision);
    }

    @Test
    void localVersionOneBehindCurrentAppliesOnlyTheLastVersion() {
        SyncDecision decision = planner.decide(Optional.of(6L), 7, 4, true);

        assertEquals(new SyncDecision.ApplyIncremental(7L, 7L), decision);
    }

    // --- Nothing cases ------------------------------------------------------

    @Test
    void localVersionAtCurrentVersionDoesNothing() {
        SyncDecision decision = planner.decide(Optional.of(7L), 7, 4, true);

        assertEquals(new SyncDecision.DoNothing(7L), decision);
    }

    @Test
    void localVersionAheadOfCurrentDoesNothingAndReportsLocalVersion() {
        SyncDecision decision = planner.decide(Optional.of(8L), 7, 4, true);

        assertEquals(new SyncDecision.DoNothing(8L), decision);
    }
}
