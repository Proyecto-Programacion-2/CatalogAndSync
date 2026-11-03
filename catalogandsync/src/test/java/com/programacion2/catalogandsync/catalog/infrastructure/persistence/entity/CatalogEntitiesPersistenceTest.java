package com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity;

import com.programacion2.catalogandsync.catalog.domain.model.DayOfWeek;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.SnapshotSourcePort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifica el mapeo real de las entidades contra el esquema V1 en la base de
 * test. El caso critico es {@code day_of_week}: si el enum PostgreSQL no se
 * bindea como tal, el fallo aparece recien al escribir (paso 6), no en
 * {@code ddl-auto=validate}.
 * <p>
 * {@code @Transactional} revierte todo lo insertado al terminar el test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogEntitiesPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SnapshotSourcePort snapshotSourcePort;

    @MockitoBean
    private CatalogWritePort catalogWritePort;

    @Test
    void enumDayOfWeekSurvivesRoundTrip() {
        ProfessionalCategoryEntity category = new ProfessionalCategoryEntity();
        category.setId(9001L);
        category.setName("Cardiologia");
        category.setEnabled(true);
        category.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.persist(category);

        ProfessionalEntity professional = new ProfessionalEntity();
        professional.setId(9002L);
        professional.setCategoryId(9001L);
        professional.setFirstName("Ana");
        professional.setLastName("Perez");
        professional.setEnabled(true);
        professional.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.persist(professional);

        WeeklyScheduleEntity schedule = new WeeklyScheduleEntity();
        schedule.setId(9003L);
        schedule.setProfessionalId(9002L);
        schedule.setDayOfWeek(DayOfWeek.WEDNESDAY);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(13, 0));
        schedule.setSlotDurationMinutes(30);
        schedule.setEnabled(true);
        schedule.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entityManager.persist(schedule);

        entityManager.flush();
        entityManager.clear();

        WeeklyScheduleEntity found = entityManager.find(WeeklyScheduleEntity.class, 9003L);

        assertNotNull(found);
        assertEquals(DayOfWeek.WEDNESDAY, found.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), found.getStartTime());
    }
}
