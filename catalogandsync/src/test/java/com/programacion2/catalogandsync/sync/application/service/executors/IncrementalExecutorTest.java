package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceException;
import com.programacion2.catalogandsync.sync.application.exception.IncrementalSourceUnavailableException;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalBatch;
import com.programacion2.catalogandsync.sync.domain.model.IncrementalResult;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.IncrementalSourcePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncrementalExecutorTest {

    @Mock
    private IncrementalSourcePort incrementalSourcePort;

    @Mock
    private CatalogWritePort catalogWritePort;

    @InjectMocks
    private IncrementalExecutor executor;

    @Test
    void appliesEveryVersionInOrderAndReturnsIncrementalOutcome() {
        IncrementalBatch version8 = batch(8);
        IncrementalBatch version9 = batch(9);
        IncrementalBatch version10 = batch(10);
        when(incrementalSourcePort.read(8)).thenReturn(Optional.of(version8));
        when(incrementalSourcePort.read(9)).thenReturn(Optional.of(version9));
        when(incrementalSourcePort.read(10)).thenReturn(Optional.of(version10));

        IncrementalResult result = executor.execute(new SyncDecision.ApplyIncremental(8, 10));

        assertEquals(new IncrementalResult.Applied(new SyncOutcome(SyncMode.INCREMENTAL, 10, false)), result);

        InOrder order = inOrder(incrementalSourcePort, catalogWritePort);
        order.verify(incrementalSourcePort).read(8);
        order.verify(catalogWritePort).applyChanges(
                8, SyncMode.INCREMENTAL, version8.categories(), version8.professionals(), version8.schedules());
        order.verify(incrementalSourcePort).read(9);
        order.verify(catalogWritePort).applyChanges(
                9, SyncMode.INCREMENTAL, version9.categories(), version9.professionals(), version9.schedules());
        order.verify(incrementalSourcePort).read(10);
        order.verify(catalogWritePort).applyChanges(
                10, SyncMode.INCREMENTAL, version10.categories(), version10.professionals(), version10.schedules());
    }

    @Test
    void appliesAnEmptyDeltaAndStillAdvancesTheVersion() {
        IncrementalBatch empty = new IncrementalBatch(8, List.of(), List.of(), List.of());
        when(incrementalSourcePort.read(8)).thenReturn(Optional.of(empty));

        IncrementalResult result = executor.execute(new SyncDecision.ApplyIncremental(8, 8));

        assertEquals(new IncrementalResult.Applied(new SyncOutcome(SyncMode.INCREMENTAL, 8, false)), result);
        verify(catalogWritePort).applyChanges(
                8, SyncMode.INCREMENTAL, List.of(), List.of(), List.of());
    }

    @Test
    void returnsNeedsSnapshotWhenTheHistoryIsMissing() {
        when(incrementalSourcePort.read(8)).thenReturn(Optional.of(batch(8)));
        when(incrementalSourcePort.read(9)).thenReturn(Optional.empty());

        IncrementalResult result = executor.execute(new SyncDecision.ApplyIncremental(8, 10));

        IncrementalResult.NeedsSnapshot needsSnapshot =
                assertInstanceOf(IncrementalResult.NeedsSnapshot.class, result);
        assertTrue(needsSnapshot.reason().contains("INCREMENTAL_HISTORY_MISSING"));
        assertTrue(needsSnapshot.reason().contains("9"));

        IncrementalBatch version8 = batch(8);
        verify(catalogWritePort).applyChanges(
                8, SyncMode.INCREMENTAL, version8.categories(), version8.professionals(), version8.schedules());
        verify(incrementalSourcePort, never()).read(10);
    }

    @Test
    void returnsNeedsSnapshotWhenADeltaBreaksTheContract() {
        when(incrementalSourcePort.read(8))
                .thenThrow(new IncrementalSourceException("delta without collections"));

        IncrementalResult result = executor.execute(new SyncDecision.ApplyIncremental(8, 10));

        IncrementalResult.NeedsSnapshot needsSnapshot =
                assertInstanceOf(IncrementalResult.NeedsSnapshot.class, result);
        assertTrue(needsSnapshot.reason().contains("INCREMENTAL_CONTRACT_BROKEN"));
        assertTrue(needsSnapshot.reason().contains("delta without collections"));
        verifyNoInteractions(catalogWritePort);
    }

    @Test
    void propagatesTransportFailuresWithoutFallingBackToSnapshot() {
        when(incrementalSourcePort.read(8))
                .thenThrow(new IncrementalSourceUnavailableException("redis down", new RuntimeException()));

        assertThrows(IncrementalSourceUnavailableException.class,
                () -> executor.execute(new SyncDecision.ApplyIncremental(8, 10)));

        verifyNoInteractions(catalogWritePort);
    }

    @Test
    void stopsTheSequenceWhenPersistenceFails() {
        IncrementalBatch version8 = batch(8);
        when(incrementalSourcePort.read(8)).thenReturn(Optional.of(version8));
        doThrow(new RuntimeException("database down"))
                .when(catalogWritePort).applyChanges(
                        8, SyncMode.INCREMENTAL, version8.categories(), version8.professionals(), version8.schedules());

        assertThrows(RuntimeException.class, () -> executor.execute(new SyncDecision.ApplyIncremental(8, 10)));

        verify(incrementalSourcePort, never()).read(9);
    }

    private static IncrementalBatch batch(long version) {
        Instant now = Instant.parse("2026-11-12T00:00:00Z");
        long base = version * 1000;
        return new IncrementalBatch(
                version,
                List.of(new ProfessionalCategory(base, "Category " + version, "desc", true, now, now)),
                List.of(new Professional(base + 1, base, "First" + version, "Last" + version, true, now, now)),
                List.of(new WeeklySchedule(base + 2, base + 1, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(17, 0), 30, true, now, now)));
    }
}
