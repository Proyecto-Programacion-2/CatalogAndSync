package com.programacion2.catalogandsync.sync.infrastructure.rest.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.SnapshotMappingException;
import com.programacion2.catalogandsync.sync.domain.model.Snapshot;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.SnapshotResponse;
import com.programacion2.catalogandsync.sync.infrastructure.rest.dto.WeeklyScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test puro del mapeo contrato REST -> dominio. No levanta HTTP ni emula a la
 * catedra: la deserializacion real se verifica manualmente contra la instancia.
 */
class SnapshotMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-10T13:40:00Z");

    private final SnapshotMapper mapper = new SnapshotMapper();

    @Test
    void mapsEveryCollectionAndField() {
        SnapshotResponse response = new SnapshotResponse(
                7L,
                Instant.parse("2026-09-10T13:44:59Z"),
                List.of(new ProfessionalCategoryResponse(
                        1L, "Clinica medica", "Atencion clinica general", true, CREATED_AT, UPDATED_AT)),
                List.of(new ProfessionalResponse(
                        15L, 1L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT)),
                List.of(new WeeklyScheduleResponse(
                        21L, 15L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(12, 0), 30,
                        true, CREATED_AT, UPDATED_AT))
        );

        Snapshot snapshot = mapper.toDomain(response);

        assertEquals(7L, snapshot.version());
        assertEquals(1, snapshot.categories().size());
        assertEquals(1, snapshot.professionals().size());
        assertEquals(1, snapshot.schedules().size());

        ProfessionalCategory category = snapshot.categories().getFirst();
        assertEquals(1L, category.id());
        assertEquals("Clinica medica", category.name());
        assertEquals("Atencion clinica general", category.description());
        assertTrue(category.enabled());
        assertEquals(CREATED_AT, category.createdAt());
        assertEquals(UPDATED_AT, category.updatedAt());

        Professional professional = snapshot.professionals().getFirst();
        assertEquals(15L, professional.id());
        assertEquals(1L, professional.categoryId());
        assertEquals("Ana", professional.firstName());
        assertEquals("Gomez", professional.lastName());
        assertTrue(professional.enabled());

        WeeklySchedule schedule = snapshot.schedules().getFirst();
        assertEquals(21L, schedule.id());
        assertEquals(15L, schedule.professionalId());
        assertEquals(DayOfWeek.MONDAY, schedule.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), schedule.startTime());
        assertEquals(LocalTime.of(12, 0), schedule.endTime());
        assertEquals(30, schedule.slotDurationMinutes());
        assertTrue(schedule.enabled());
        assertEquals(CREATED_AT, schedule.createdAt());
    }

    @Test
    void acceptsNullDescription() {
        SnapshotResponse response = new SnapshotResponse(
                1L,
                null,
                List.of(new ProfessionalCategoryResponse(1L, "Clinica", null, true, null, null)),
                List.of(),
                List.of()
        );

        assertNull(mapper.toDomain(response).categories().getFirst().description());
    }

    @Test
    void rejectsNullResponse() {
        assertThrows(SnapshotMappingException.class, () -> mapper.toDomain(null));
    }

    @Test
    void rejectsNonPositiveSnapshotVersion() {
        assertThrows(SnapshotMappingException.class, () -> mapper.toDomain(snapshot(0L, List.of(), List.of(), List.of())));
        assertThrows(SnapshotMappingException.class, () -> mapper.toDomain(snapshot(null, List.of(), List.of(), List.of())));
    }

    @Test
    void rejectsMissingCollectionsInsteadOfTreatingThemAsEmpty() {
        assertThrows(SnapshotMappingException.class,
                () -> mapper.toDomain(snapshot(1L, null, List.of(), List.of())));
        assertThrows(SnapshotMappingException.class,
                () -> mapper.toDomain(snapshot(1L, List.of(), null, List.of())));
        assertThrows(SnapshotMappingException.class,
                () -> mapper.toDomain(snapshot(1L, List.of(), List.of(), null)));
    }

    @Test
    void rejectsUnknownDayOfWeek() {
        WeeklyScheduleResponse schedule = new WeeklyScheduleResponse(
                21L, 15L, "FUNDAY", LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, CREATED_AT, UPDATED_AT);

        assertThrows(SnapshotMappingException.class,
                () -> mapper.toDomain(snapshot(1L, List.of(), List.of(professional()), List.of(schedule))));
    }

    @Test
    void rejectsMissingRequiredFields() {
        ProfessionalCategoryResponse categoryWithoutId =
                new ProfessionalCategoryResponse(null, "Clinica", null, true, null, null);

        assertThrows(SnapshotMappingException.class,
                () -> mapper.toDomain(snapshot(1L, List.of(categoryWithoutId), List.of(), List.of())));
    }

    private SnapshotResponse snapshot(
            Long version,
            List<ProfessionalCategoryResponse> categories,
            List<ProfessionalResponse> professionals,
            List<WeeklyScheduleResponse> schedules) {
        return new SnapshotResponse(version, null, categories, professionals, schedules);
    }

    private ProfessionalResponse professional() {
        return new ProfessionalResponse(15L, 1L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT);
    }
}
