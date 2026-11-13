package com.programacion2.catalogandsync.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Invariantes de dominio de {@link Professional}. Son independientes de la
 * fuente: valen para REST, Redis y cualquier camino futuro.
 */
class ProfessionalTest {

    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");

    @Test
    void acceptsPositiveIds() {
        Professional professional = new Professional(15, 1, "Ana", "Gomez", true, NOW, NOW);

        assertEquals(15, professional.id());
        assertEquals(1, professional.categoryId());
    }

    @Test
    void rejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Professional(0, 1, "Ana", "Gomez", true, NOW, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Professional(-1, 1, "Ana", "Gomez", true, NOW, NOW));
    }

    @Test
    void rejectsNonPositiveCategoryId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Professional(15, 0, "Ana", "Gomez", true, NOW, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new Professional(15, -1, "Ana", "Gomez", true, NOW, NOW));
    }
}
