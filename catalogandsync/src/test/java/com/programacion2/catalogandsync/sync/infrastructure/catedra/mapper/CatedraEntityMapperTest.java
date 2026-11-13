package com.programacion2.catalogandsync.sync.infrastructure.catedra.mapper;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.CatedraDayOfWeek;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalCategoryResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.ProfessionalResponse;
import com.programacion2.catalogandsync.sync.infrastructure.catedra.dto.WeeklyScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test puro de la traduccion contrato de cable -> dominio compartida por REST y
 * Redis. No levanta HTTP ni emula a la catedra: la deserializacion real se
 * verifica manualmente contra la instancia.
 */
class CatedraEntityMapperTest {

    private static final String CONTEXT = "Catedra test schedules[]";
    private static final Instant CREATED_AT = Instant.parse("2026-07-01T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-10T13:40:00Z");

    @Test
    void mapsEveryDayOfWeek() {
        for (CatedraDayOfWeek day : CatedraDayOfWeek.values()) {
            WeeklySchedule schedule = CatedraEntityMapper.toDomain(
                    schedule(21L, 15L, day, LocalTime.of(9, 0), LocalTime.of(13, 0), 30), CONTEXT);

            assertEquals(DayOfWeek.valueOf(day.name()), schedule.dayOfWeek());
        }
    }

    @Test
    void mapsCategoryFields() {
        ProfessionalCategory category = CatedraEntityMapper.toDomain(
                new ProfessionalCategoryResponse(1L, "Clinica medica", "Atencion general", true, CREATED_AT, UPDATED_AT),
                CONTEXT);

        assertEquals(1L, category.id());
        assertEquals("Clinica medica", category.name());
        assertEquals("Atencion general", category.description());
        assertTrue(category.enabled());
        assertEquals(CREATED_AT, category.createdAt());
        assertEquals(UPDATED_AT, category.updatedAt());
    }

    @Test
    void mapsProfessionalFields() {
        Professional professional = CatedraEntityMapper.toDomain(
                new ProfessionalResponse(15L, 1L, "Ana", "Gomez", true, CREATED_AT, UPDATED_AT), CONTEXT);

        assertEquals(15L, professional.id());
        assertEquals(1L, professional.categoryId());
        assertEquals("Ana", professional.firstName());
        assertEquals("Gomez", professional.lastName());
        assertTrue(professional.enabled());
    }

    @Test
    void acceptsNullOptionalFields() {
        ProfessionalCategory category = CatedraEntityMapper.toDomain(
                new ProfessionalCategoryResponse(1L, "Clinica", null, true, null, null), CONTEXT);

        assertNull(category.description());
        assertNull(category.createdAt());
    }

    @Test
    void rejectsNullResponses() {
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain((ProfessionalCategoryResponse) null, CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain((ProfessionalResponse) null, CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain((WeeklyScheduleResponse) null, CONTEXT));
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        new ProfessionalCategoryResponse(null, "Clinica", null, true, null, null), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        new ProfessionalResponse(15L, null, "Ana", "Gomez", true, null, null), CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        schedule(21L, 15L, CatedraDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), null),
                        CONTEXT));
        assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        new WeeklyScheduleResponse(21L, 15L, CatedraDayOfWeek.MONDAY, LocalTime.of(9, 0),
                                LocalTime.of(13, 0), 30, true, null, null),
                        CONTEXT));
    }

    @Test
    void rejectsNullDayOfWeek() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        schedule(21L, 15L, null, LocalTime.of(9, 0), LocalTime.of(13, 0), 30), CONTEXT));

        assertTrue(ex.getMessage().contains("dayOfWeek"));
    }

    @Test
    void translatesDomainInvariantViolationWithChannelContext() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        schedule(0L, 15L, CatedraDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), 30),
                        CONTEXT));

        assertTrue(ex.getMessage().startsWith(CONTEXT));
        assertTrue(ex.getMessage().contains("is invalid"));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void rejectsInvertedWindow() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        schedule(21L, 15L, CatedraDayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(9, 0), 30),
                        CONTEXT));

        assertTrue(ex.getMessage().contains("must be before endTime"));
    }

    @Test
    void rejectsNonPositiveSlotDuration() {
        CatedraContractException ex = assertThrows(CatedraContractException.class,
                () -> CatedraEntityMapper.toDomain(
                        schedule(21L, 15L, CatedraDayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), 0),
                        CONTEXT));

        assertTrue(ex.getMessage().contains("slotDurationMinutes"));
    }

    private static WeeklyScheduleResponse schedule(Long id, Long professionalId, CatedraDayOfWeek dayOfWeek,
                                                   LocalTime startTime, LocalTime endTime, Integer slotDurationMinutes) {
        return new WeeklyScheduleResponse(id, professionalId, dayOfWeek, startTime, endTime, slotDurationMinutes,
                true, CREATED_AT, UPDATED_AT);
    }
}
