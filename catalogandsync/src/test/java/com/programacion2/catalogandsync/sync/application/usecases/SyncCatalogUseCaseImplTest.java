package com.programacion2.catalogandsync.sync.application.usecases;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogConsistencyPort;
import com.programacion2.catalogandsync.sync.application.exception.SyncException;
import com.programacion2.catalogandsync.sync.application.service.SyncPlanner;
import com.programacion2.catalogandsync.sync.application.service.executors.IncrementalExecutor;
import com.programacion2.catalogandsync.sync.application.service.executors.NothingExecutor;
import com.programacion2.catalogandsync.sync.application.service.executors.SnapshotExecutor;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncCatalogUseCaseImplTest {

    @Mock
    private SyncPlanner syncPlanner;

    @Mock
    private VersionLedgerPort versionLedgerPort;

    @Mock
    private CatalogConsistencyPort catalogConsistencyPort;

    @Mock
    private NothingExecutor nothingExecutor;

    @Mock
    private IncrementalExecutor incrementalExecutor;

    @Mock
    private SnapshotExecutor snapshotExecutor;

    @InjectMocks
    private SyncCatalogUseCaseImpl syncCatalogUseCase;

    @Test
    void whenPlannerDecidesDoNothing_shouldExecuteNothingExecutor() {
        Optional<Long> localVersion = Optional.of(5L);
        long currentVersion = 5L;
        long oldestVersion = 1L;
        SyncDecision.DoNothing decision = new SyncDecision.DoNothing(5L);
        SyncOutcome expectedOutcome = new SyncOutcome(SyncMode.NOTHING, 5L, true);

        when(versionLedgerPort.localVersion()).thenReturn(localVersion);
        when(versionLedgerPort.currentVersion()).thenReturn(currentVersion);
        when(versionLedgerPort.oldestAvailableVersion()).thenReturn(oldestVersion);
        when(catalogConsistencyPort.isStateVerifiable(localVersion)).thenReturn(true);
        when(syncPlanner.decide(localVersion, currentVersion, oldestVersion, true)).thenReturn(decision);
        when(nothingExecutor.execute(decision)).thenReturn(expectedOutcome);

        SyncOutcome outcome = syncCatalogUseCase.sync();

        assertEquals(expectedOutcome, outcome);
        verify(nothingExecutor).execute(decision);
        verifyNoInteractions(snapshotExecutor, incrementalExecutor);
    }

    @Test
    void whenPlannerDecidesTakeSnapshot_shouldExecuteSnapshotExecutor() {
        Optional<Long> localVersion = Optional.empty();
        long currentVersion = 10L;
        long oldestVersion = 5L;
        SyncDecision.TakeSnapshot decision = new SyncDecision.TakeSnapshot("Empty local database");
        SyncOutcome expectedOutcome = new SyncOutcome(SyncMode.SNAPSHOT, 10L, false);

        when(versionLedgerPort.localVersion()).thenReturn(localVersion);
        when(versionLedgerPort.currentVersion()).thenReturn(currentVersion);
        when(versionLedgerPort.oldestAvailableVersion()).thenReturn(oldestVersion);
        when(catalogConsistencyPort.isStateVerifiable(localVersion)).thenReturn(false);
        when(syncPlanner.decide(localVersion, currentVersion, oldestVersion, false)).thenReturn(decision);
        when(snapshotExecutor.execute(decision)).thenReturn(expectedOutcome);

        SyncOutcome outcome = syncCatalogUseCase.sync();

        assertEquals(expectedOutcome, outcome);
        verify(snapshotExecutor).execute(decision);
        verifyNoInteractions(nothingExecutor, incrementalExecutor);
    }

    @Test
    void whenPlannerDecidesApplyIncremental_shouldExecuteIncrementalExecutor() {
        Optional<Long> localVersion = Optional.of(7L);
        long currentVersion = 10L;
        long oldestVersion = 5L;
        SyncDecision.ApplyIncremental decision = new SyncDecision.ApplyIncremental(8L, 10L);
        SyncOutcome expectedOutcome = new SyncOutcome(SyncMode.INCREMENTAL, 10L, false);

        when(versionLedgerPort.localVersion()).thenReturn(localVersion);
        when(versionLedgerPort.currentVersion()).thenReturn(currentVersion);
        when(versionLedgerPort.oldestAvailableVersion()).thenReturn(oldestVersion);
        when(catalogConsistencyPort.isStateVerifiable(localVersion)).thenReturn(true);
        when(syncPlanner.decide(localVersion, currentVersion, oldestVersion, true)).thenReturn(decision);
        when(incrementalExecutor.execute(decision)).thenReturn(expectedOutcome);

        SyncOutcome outcome = syncCatalogUseCase.sync();

        assertEquals(expectedOutcome, outcome);
        verify(incrementalExecutor).execute(decision);
        verifyNoInteractions(nothingExecutor, snapshotExecutor);
    }

    @Test
    void whenVersionLedgerPortFails_shouldThrowSyncException() {
        when(versionLedgerPort.localVersion()).thenThrow(new RuntimeException("Database error reading local version"));

        SyncException exception = assertThrows(SyncException.class, () -> syncCatalogUseCase.sync());

        assertEquals("Error in the sync process", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        verifyNoInteractions(syncPlanner, nothingExecutor, snapshotExecutor, incrementalExecutor);
    }

    @Test
    void whenCatalogConsistencyPortFails_shouldThrowSyncException() {
        Optional<Long> localVersion = Optional.of(3L);
        when(versionLedgerPort.localVersion()).thenReturn(localVersion);
        when(versionLedgerPort.currentVersion()).thenReturn(5L);
        when(versionLedgerPort.oldestAvailableVersion()).thenReturn(1L);
        when(catalogConsistencyPort.isStateVerifiable(localVersion))
                .thenThrow(new IllegalStateException("Consistency check unavailable"));

        SyncException exception = assertThrows(SyncException.class, () -> syncCatalogUseCase.sync());

        assertEquals("Error in the sync process", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        verifyNoInteractions(syncPlanner, nothingExecutor, snapshotExecutor, incrementalExecutor);
    }

    @Test
    void whenExecutorThrowsException_shouldThrowSyncException() {
        Optional<Long> localVersion = Optional.empty();
        long currentVersion = 10L;
        long oldestVersion = 5L;
        SyncDecision.TakeSnapshot decision = new SyncDecision.TakeSnapshot("Empty database");

        when(versionLedgerPort.localVersion()).thenReturn(localVersion);
        when(versionLedgerPort.currentVersion()).thenReturn(currentVersion);
        when(versionLedgerPort.oldestAvailableVersion()).thenReturn(oldestVersion);
        when(catalogConsistencyPort.isStateVerifiable(localVersion)).thenReturn(false);
        when(syncPlanner.decide(localVersion, currentVersion, oldestVersion, false)).thenReturn(decision);
        when(snapshotExecutor.execute(decision)).thenThrow(new RuntimeException("Snapshot failed"));

        SyncException exception = assertThrows(SyncException.class, () -> syncCatalogUseCase.sync());

        assertEquals("Error in the sync process", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals("Snapshot failed", exception.getCause().getMessage());
    }
}
