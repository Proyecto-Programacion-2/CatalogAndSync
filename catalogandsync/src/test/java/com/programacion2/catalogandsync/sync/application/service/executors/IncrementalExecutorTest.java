package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalExecutorTest {

    private final IncrementalExecutor executor = new IncrementalExecutor();

    @Test
    void failsExplicitlyUntilIncrementalIsImplemented() {
        SyncDecision.ApplyIncremental decision = new SyncDecision.ApplyIncremental(4, 7);

        SyncException exception = assertThrows(SyncException.class, () -> executor.execute(decision));

        assertTrue(exception.getMessage().contains("INCREMENTAL_UNAVAILABLE"));
        assertTrue(exception.getMessage().contains("fromVersion=4"));
        assertTrue(exception.getMessage().contains("toVersion=7"));
    }
}
