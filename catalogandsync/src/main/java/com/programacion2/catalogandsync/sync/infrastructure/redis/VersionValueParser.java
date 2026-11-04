package com.programacion2.catalogandsync.sync.infrastructure.redis;

import com.programacion2.catalogandsync.sync.application.exception.VersionSourceException;

/**
 * Convierte el valor crudo de una clave de version (String decimal, IR §14.2)
 * al {@code long} del dominio.
 * <p>
 * Es puro a proposito: se testea sin Redis. La catedra no se emula; la lectura
 * real se verifica manualmente contra la instancia.
 */
public final class VersionValueParser {

    private VersionValueParser() {
    }

    public static long parse(String raw, String key) {
        if (raw == null) {
            throw new VersionSourceException("Catedra Redis key '" + key + "' is missing");
        }

        String value = raw.strip();
        if (value.isEmpty()) {
            throw new VersionSourceException("Catedra Redis key '" + key + "' is empty");
        }

        long version;
        try {
            version = Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new VersionSourceException(
                    "Catedra Redis key '" + key + "' is not a decimal version: '" + value + "'", ex);
        }

        if (version < 0) {
            throw new VersionSourceException(
                    "Catedra Redis key '" + key + "' has a negative version: " + version);
        }
        return version;
    }
}
