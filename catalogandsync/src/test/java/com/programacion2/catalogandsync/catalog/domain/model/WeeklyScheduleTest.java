package com.programacion2.catalogandsync.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Invariantes de dominio de {@link WeeklySchedule}: identidad positiva, ventana
 * horaria del mismo dia y duracion de slot positiva.
 */
class WeeklyScheduleTest {

    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(13, 0);

    @Test
    void acceptsAValidWindow() {
        WeeklySchedule schedule = schedule(21, 15, DayOfWeek.MONDAY, START, END, 30);

        assertEquals(21, schedule.id());
        assertEquals(DayOfWeek.MONDAY, schedule.dayOfWeek());
    }

    @Test
    void rejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class,
                () -> schedule(0, 15, DayOfWeek.MONDAY, START, END, 30));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(-1, 15, DayOfWeek.MONDAY, START, END, 30));
    }

    @Test
    void rejectsNonPositiveProfessionalId() {
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 0, DayOfWeek.MONDAY, START, END, 30));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, -1, DayOfWeek.MONDAY, START, END, 30));
    }

    @Test
    void rejectsNullDayOfWeekAndTimes() {
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, null, START, END, 30));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, null, END, 30));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, START, null, 30));
    }

    @Test
    void rejectsEmptyOrInvertedWindow() {
        // start == end y start > end: no se admite cruce de medianoche (A1).
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, START, START, 30));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, LocalTime.of(13, 0), START, 30));
    }

    @Test
    void rejectsNonPositiveSlotDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, START, END, 0));
        assertThrows(IllegalArgumentException.class,
                () -> schedule(21, 15, DayOfWeek.MONDAY, START, END, -30));
    }

    private static WeeklySchedule schedule(long id, long professionalId, DayOfWeek dayOfWeek,
                                           LocalTime start, LocalTime end, int slotDurationMinutes) {
        return new WeeklySchedule(id, professionalId, dayOfWeek, start, end, slotDurationMinutes, true, NOW, NOW);
    }
}
