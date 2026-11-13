package com.programacion2.catalogandsync.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Invariantes de dominio de {@link ProfessionalCategory}. Son independientes de
 * la fuente: valen para REST, Redis y cualquier camino futuro.
 */
class ProfessionalCategoryTest {

    private static final Instant NOW = Instant.parse("2026-11-13T00:00:00Z");

    @Test
    void acceptsPositiveId() {
        ProfessionalCategory category = new ProfessionalCategory(1, "Clinica medica", "desc", true, NOW, NOW);

        assertEquals(1, category.id());
        assertEquals("Clinica medica", category.name());
    }

    @Test
    void acceptsNullDescriptionAndTimestamps() {
        ProfessionalCategory category = new ProfessionalCategory(1, "Clinica medica", null, true, null, null);

        assertEquals(1, category.id());
    }

    @Test
    void rejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProfessionalCategory(0, "Clinica medica", null, true, NOW, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> new ProfessionalCategory(-1, "Clinica medica", null, true, NOW, NOW));
    }
}
