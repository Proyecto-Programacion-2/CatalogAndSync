package com.programacion2.catalogandsync.sync.infrastructure.redis;

import com.programacion2.catalogandsync.sync.application.exception.VersionSourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test puro del parseo de las claves de version (IR §14.2). No toca Redis: la
 * lectura real se verifica manualmente contra la instancia de la catedra.
 */
class VersionValueParserTest {

    private static final String KEY = "catedra:sync:current-version";

    @Test
    void parsesDecimalValue() {
        assertEquals(7L, VersionValueParser.parse("7", KEY));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(42L, VersionValueParser.parse("  42  ", KEY));
    }

    @Test
    void acceptsZero() {
        assertEquals(0L, VersionValueParser.parse("0", KEY));
    }

    @Test
    void rejectsMissingKey() {
        assertThrows(VersionSourceException.class, () -> VersionValueParser.parse(null, KEY));
    }

    @Test
    void rejectsEmptyValue() {
        assertThrows(VersionSourceException.class, () -> VersionValueParser.parse("   ", KEY));
    }

    @Test
    void rejectsNonNumericValue() {
        assertThrows(VersionSourceException.class, () -> VersionValueParser.parse("seven", KEY));
    }

    @Test
    void rejectsNegativeValue() {
        assertThrows(VersionSourceException.class, () -> VersionValueParser.parse("-1", KEY));
    }
}
