package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el contrato de escritura sobre la base de test: upsert por ID,
 * baja logica de los IDs ausentes del snapshot, delta incremental sin bajas
 * implicitas y avance de version en la misma transaccion. El catalogo es un
 * espejo: el estado de un padre no se propaga a sus hijos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaCatalogWriteAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Autowired
    private JpaCatalogWriteAdapter adapter;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void appliesVersionFromEmptyDatabase() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 1, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        );

        assertTrue(category(1).isEnabled());
        assertEquals("Cardiologia", category(1).getName());

        ProfessionalEntity professional = professional(10);
        assertNotNull(professional);
        assertTrue(professional.isEnabled());
        assertEquals(1L, professional.getCategoryId());

        WeeklyScheduleEntity schedule = schedule(100);
        assertNotNull(schedule);
        assertTrue(schedule.isEnabled());
        assertEquals(DayOfWeek.MONDAY, schedule.getDayOfWeek());

        CatalogVersionEntity version = entityManager.find(CatalogVersionEntity.class, (short) 1);
        assertNotNull(version);
        assertEquals(7L, version.getLocalVersion());
        assertEquals("SNAPSHOT", version.getLastSyncMode());
    }

    @Test
    void updatesExistingRowsAndDisablesMissingIds() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true), category(2, "Dermatologia", true)),
                List.of(professional(10, 1, "Ana", true), professional(20, 2, "Beto", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true), schedule(200, 20, DayOfWeek.TUESDAY, true))
        );

        adapter.applyVersion(
                8,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia Infantil", true)),
                List.of(professional(10, 1, "Ana Maria", true)),
                List.of(schedule(100, 10, DayOfWeek.WEDNESDAY, true))
        );

        // Upsert: los IDs que siguen vienen actualizados.
        assertEquals("Cardiologia Infantil", category(1).getName());
        assertEquals("Ana Maria", professional(10).getFirstName());
        assertEquals(DayOfWeek.WEDNESDAY, schedule(100).getDayOfWeek());

        // Categoria ausente: baja logica, sin borrado fisico.
        ProfessionalCategoryEntity missingCategory = category(2);
        assertNotNull(missingCategory);
        assertFalse(missingCategory.isEnabled());

        // Profesional y horario ausentes: baja logica.
        assertFalse(professional(20).isEnabled());
        assertFalse(schedule(200).isEnabled());

        // Lo que siguio vigente no se toca.
        assertTrue(category(1).isEnabled());
        assertTrue(professional(10).isEnabled());
        assertTrue(schedule(100).isEnabled());

        assertEquals(8L, entityManager.find(CatalogVersionEntity.class, (short) 1).getLocalVersion());
    }

    @Test
    void keepsDependentFlagsFromSourceWhenParentIsDisabled() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 1, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        );

        // La categoria se deshabilita, pero sus hijos siguen presentes y
        // habilitados en la fuente: el catalogo local no inventa la baja.
        adapter.applyVersion(
                8,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", false)),
                List.of(professional(10, 1, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        );

        assertFalse(category(1).isEnabled());
        assertTrue(professional(10).isEnabled());
        assertTrue(schedule(100).isEnabled());
    }

    @Test
    void emptySnapshotDisablesEverything() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 1, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        );

        adapter.applyVersion(8, SyncMode.SNAPSHOT, List.of(), List.of(), List.of());

        assertFalse(category(1).isEnabled());
        assertFalse(professional(10).isEnabled());
        assertFalse(schedule(100).isEnabled());
        assertEquals(8L, entityManager.find(CatalogVersionEntity.class, (short) 1).getLocalVersion());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollsBackTheWholeUnitOfWorkWhenOneRowFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            entityManager.createQuery("delete from WeeklyScheduleEntity").executeUpdate();
            entityManager.createQuery("delete from ProfessionalEntity").executeUpdate();
            entityManager.createQuery("delete from ProfessionalCategoryEntity").executeUpdate();
            entityManager.createQuery("delete from CatalogVersionEntity").executeUpdate();
        });

        // El profesional apunta a una categoria inexistente: la FK falla al hacer flush.
        assertThrows(PersistenceException.class, () -> adapter.applyVersion(
                9,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 999, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        ));

        transaction.executeWithoutResult(status -> {
            assertNull(entityManager.find(ProfessionalCategoryEntity.class, 1L));
            assertNull(entityManager.find(ProfessionalEntity.class, 10L));
            assertNull(entityManager.find(WeeklyScheduleEntity.class, 100L));
            assertNull(entityManager.find(CatalogVersionEntity.class, (short) 1));
        });
    }

    @Test
    void applyChangesUpsertsDeltaWithoutTouchingRowsOutsideIt() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true), category(2, "Dermatologia", true)),
                List.of(professional(10, 1, "Ana", true), professional(20, 2, "Beto", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true), schedule(200, 20, DayOfWeek.TUESDAY, true))
        );

        // Delta de una sola version: actualiza un profesional y no menciona al resto.
        adapter.applyChanges(
                8,
                SyncMode.INCREMENTAL,
                List.of(),
                List.of(professional(10, 1, "Ana Maria", false)),
                List.of()
        );

        assertEquals("Ana Maria", professional(10).getFirstName());
        assertFalse(professional(10).isEnabled());

        // Los IDs fuera del delta conservan su estado: un delta no da de baja.
        assertEquals("Dermatologia", category(2).getName());
        assertTrue(category(2).isEnabled());
        assertTrue(professional(20).isEnabled());
        assertTrue(schedule(200).isEnabled());
        assertTrue(schedule(100).isEnabled());

        CatalogVersionEntity version = entityManager.find(CatalogVersionEntity.class, (short) 1);
        assertEquals(8L, version.getLocalVersion());
        assertEquals("INCREMENTAL", version.getLastSyncMode());
    }

    @Test
    void applyChangesWithEmptyDeltaStillAdvancesVersion() {
        adapter.applyVersion(
                7,
                SyncMode.SNAPSHOT,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 1, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        );

        adapter.applyChanges(8, SyncMode.INCREMENTAL, List.of(), List.of(), List.of());

        assertTrue(category(1).isEnabled());
        assertTrue(professional(10).isEnabled());
        assertTrue(schedule(100).isEnabled());

        CatalogVersionEntity version = entityManager.find(CatalogVersionEntity.class, (short) 1);
        assertEquals(8L, version.getLocalVersion());
        assertEquals("INCREMENTAL", version.getLastSyncMode());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void applyChangesRollsBackTheWholeUnitOfWorkWhenOneRowFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            entityManager.createQuery("delete from WeeklyScheduleEntity").executeUpdate();
            entityManager.createQuery("delete from ProfessionalEntity").executeUpdate();
            entityManager.createQuery("delete from ProfessionalCategoryEntity").executeUpdate();
            entityManager.createQuery("delete from CatalogVersionEntity").executeUpdate();
        });

        // El delta referencia una categoria inexistente: la FK falla al hacer flush.
        assertThrows(PersistenceException.class, () -> adapter.applyChanges(
                9,
                SyncMode.INCREMENTAL,
                List.of(category(1, "Cardiologia", true)),
                List.of(professional(10, 999, "Ana", true)),
                List.of(schedule(100, 10, DayOfWeek.MONDAY, true))
        ));

        transaction.executeWithoutResult(status -> {
            assertNull(entityManager.find(ProfessionalCategoryEntity.class, 1L));
            assertNull(entityManager.find(ProfessionalEntity.class, 10L));
            assertNull(entityManager.find(WeeklyScheduleEntity.class, 100L));
            assertNull(entityManager.find(CatalogVersionEntity.class, (short) 1));
        });
    }

    private ProfessionalCategoryEntity category(long id) {
        return entityManager.find(ProfessionalCategoryEntity.class, id);
    }

    private ProfessionalEntity professional(long id) {
        return entityManager.find(ProfessionalEntity.class, id);
    }

    private WeeklyScheduleEntity schedule(long id) {
        return entityManager.find(WeeklyScheduleEntity.class, id);
    }

    private ProfessionalCategory category(long id, String name, boolean enabled) {
        return new ProfessionalCategory(id, name, null, enabled, CREATED_AT, UPDATED_AT);
    }

    private Professional professional(long id, long categoryId, String firstName, boolean enabled) {
        return new Professional(id, categoryId, firstName, "Perez", enabled, CREATED_AT, UPDATED_AT);
    }

    private WeeklySchedule schedule(long id, long professionalId, DayOfWeek dayOfWeek, boolean enabled) {
        return new WeeklySchedule(id, professionalId, dayOfWeek,
                LocalTime.of(9, 0), LocalTime.of(13, 0), 30, enabled, CREATED_AT, UPDATED_AT);
    }
}
