package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el check 1 y la consistencia de las colecciones (check 4) de
 * DISENO_SYNC §3 contra PostgreSQL real (base de test). La transaccion de test
 * limpia lo insertado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcCatalogConsistencyAdapterTest {

    @Autowired
    private JdbcCatalogConsistencyAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void emptyLocalVersionIsNotVerifiable() {
        assertFalse(adapter.isStateVerifiable(Optional.empty()));
    }

    @Test
    void versionWithLegitimatelyEmptyCatalogIsVerifiable() {
        deleteCatalogRows();

        assertTrue(adapter.isStateVerifiable(Optional.of(7L)));
    }

    @Test
    void versionWithTheThreeCollectionsIsVerifiable() {
        deleteCatalogRows();
        insertCategory(1);
        insertProfessional(1, 1);
        insertSchedule(1, 1);

        assertTrue(adapter.isStateVerifiable(Optional.of(7L)));
    }

    @Test
    void versionWithOneMissingCollectionIsNotVerifiable() {
        deleteCatalogRows();
        insertCategory(1);
        insertProfessional(1, 1);

        assertFalse(adapter.isStateVerifiable(Optional.of(7L)));
    }

    private void deleteCatalogRows() {
        jdbcTemplate.update("DELETE FROM catalog.weekly_schedule");
        jdbcTemplate.update("DELETE FROM catalog.professional");
        jdbcTemplate.update("DELETE FROM catalog.professional_category");
    }

    private void insertCategory(long id) {
        jdbcTemplate.update(
                "INSERT INTO catalog.professional_category (id, name, enabled) VALUES (?, ?, true)",
                id, "Cardiologia");
    }

    private void insertProfessional(long id, long categoryId) {
        jdbcTemplate.update("""
                INSERT INTO catalog.professional (id, category_id, first_name, last_name, enabled)
                VALUES (?, ?, 'Ana', 'Perez', true)
                """, id, categoryId);
    }

    private void insertSchedule(long id, long professionalId) {
        jdbcTemplate.update("""
                INSERT INTO catalog.weekly_schedule
                    (id, professional_id, day_of_week, start_time, end_time,
                     slot_duration_minutes, enabled, created_at)
                VALUES (?, ?, 'MONDAY', '09:00', '12:00', 30, true, now())
                """, id, professionalId);
    }
}
