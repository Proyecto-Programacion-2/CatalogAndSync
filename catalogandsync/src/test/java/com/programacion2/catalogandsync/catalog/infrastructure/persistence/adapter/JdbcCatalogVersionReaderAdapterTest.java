package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el lector de version contra PostgreSQL real (base de test). La
 * transaccion de test limpia lo insertado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcCatalogVersionReaderAdapterTest {

    @Autowired
    private JdbcCatalogVersionReaderAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsTheLocalVersionFromTheSingleRow() {
        jdbcTemplate.update("DELETE FROM catalog.catalog_version WHERE id = 1");
        jdbcTemplate.update("""
                INSERT INTO catalog.catalog_version (id, local_version, last_sync_mode)
                VALUES (1, 7, 'SNAPSHOT')
                """);

        assertEquals(Optional.of(7L), adapter.readVersion());
    }

    @Test
    void returnsEmptyWhenTheVersionRowDoesNotExist() {
        jdbcTemplate.update("DELETE FROM catalog.catalog_version WHERE id = 1");

        assertTrue(adapter.readVersion().isEmpty());
    }
}
