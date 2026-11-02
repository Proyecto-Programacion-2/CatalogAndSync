package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotExecutorTest {

    @Mock
    private SnapshotSourcePort snapshotSourcePort;

    @Mock
    private CatalogWritePort catalogWritePort;

    @InjectMocks
    private SnapshotExecutor snapshotExecutor;

    @Test
    void executeSuccessfullyFetchesSnapshotAndAppliesVersion() {
        // Arrange
        SyncDecision.TakeSnapshot decision = new SyncDecision.TakeSnapshot("Empty database");
        Instant now = Instant.now();

        List<ProfessionalCategory> categories = List.of(
                new ProfessionalCategory(1L, "Medicina General", "Atención primaria", true, now, now)
        );
        List<Professional> professionals = List.of(
                new Professional(10L, 1L, "Gregory", "House", true, now, now)
        );
        List<WeeklySchedule> schedules = List.of(
                new WeeklySchedule(100L, 10L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), 30, true, now, now)
        );
        Snapshot snapshot = new Snapshot(42L, categories, professionals, schedules);

        when(snapshotSourcePort.fetchSnapshot()).thenReturn(snapshot);

        // Act
        SyncOutcome outcome = snapshotExecutor.execute(decision);

        // Assert
        SyncOutcome expectedOutcome = new SyncOutcome(SyncMode.SNAPSHOT, 42L, false);
        assertEquals(expectedOutcome, outcome);

        verify(snapshotSourcePort).fetchSnapshot();
        verify(catalogWritePort).applyVersion(42L, SyncMode.SNAPSHOT, categories, professionals, schedules);
    }

    @Test
    void executeWhenSourceFailsDoesNotCallCatalogWritePortAndPropagatesException() {
        // Arrange
        SyncDecision.TakeSnapshot decision = new SyncDecision.TakeSnapshot("Discontinuity detected");
        when(snapshotSourcePort.fetchSnapshot()).thenThrow(new RuntimeException("Cátedra API timeout"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> snapshotExecutor.execute(decision));
        assertEquals("Cátedra API timeout", exception.getMessage());

        verify(snapshotSourcePort).fetchSnapshot();
        verifyNoInteractions(catalogWritePort);
    }

    @Test
    void executeWhenPersistenceFailsPropagatesException() {
        // Arrange
        SyncDecision.TakeSnapshot decision = new SyncDecision.TakeSnapshot("Unverifiable local state");
        Snapshot snapshot = new Snapshot(10L, List.of(), List.of(), List.of());
        when(snapshotSourcePort.fetchSnapshot()).thenReturn(snapshot);
        doThrow(new RuntimeException("Database error applying snapshot"))
                .when(catalogWritePort).applyVersion(10L, SyncMode.SNAPSHOT, List.of(), List.of(), List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> snapshotExecutor.execute(decision));
        assertEquals("Database error applying snapshot", exception.getMessage());

        verify(snapshotSourcePort).fetchSnapshot();
        verify(catalogWritePort).applyVersion(10L, SyncMode.SNAPSHOT, List.of(), List.of(), List.of());
    }
}
