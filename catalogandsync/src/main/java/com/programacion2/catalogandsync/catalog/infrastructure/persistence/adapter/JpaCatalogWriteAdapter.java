package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.model.Professional;
import com.programacion2.catalogandsync.catalog.domain.model.ProfessionalCategory;
import com.programacion2.catalogandsync.catalog.domain.model.WeeklySchedule;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.CatalogVersionEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import com.programacion2.catalogandsync.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Unica via de escritura del catalogo (R4 de SYNC_RESPONSABILIDADES.md).
 * Snapshot e incremental usan este adaptador para que los datos y la version
 * avancen en la misma transaccion.
 * <p>
 * Orden de escritura: upsert de categorias -> profesionales -> horarios, y
 * luego bajas logicas en orden inverso. Las bajas cascadean: una categoria
 * deshabilitada deshabilita sus profesionales, y un profesional deshabilitado
 * deshabilita sus horarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaCatalogWriteAdapter implements CatalogWritePort {

    private static final short SINGLETON_ID = 1;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void applyVersion(long version, SyncMode mode, List<ProfessionalCategory> categories,
                             List<Professional> professionals, List<WeeklySchedule> schedules) {

        categories.forEach(category -> entityManager.merge(toEntity(category)));
        professionals.forEach(professional -> entityManager.merge(toEntity(professional)));
        schedules.forEach(schedule -> entityManager.merge(toEntity(schedule)));

        // Necesario para que las bajas logicas vean lo recien insertado/actualizado.
        entityManager.flush();

        int disabledCategories = disableMissingCategories(categories);
        int disabledProfessionals = disableProfessionals(professionals);
        int disabledSchedules = disableSchedules(schedules);

        // Los bulk update no actualizan el contexto de persistencia.
        entityManager.clear();

        updateCatalogVersion(version, mode);

        log.info("Applied catalog version {} ({}): categories={} (+{} disabled), professionals={} (+{} disabled), schedules={} (+{} disabled)",
                version,
                mode,
                categories.size(),
                disabledCategories,
                professionals.size(),
                disabledProfessionals,
                schedules.size(),
                disabledSchedules);
    }

    private int disableMissingCategories(List<ProfessionalCategory> categories) {
        if (categories.isEmpty()) {
            return entityManager.createQuery(
                            "update ProfessionalCategoryEntity c set c.enabled = false where c.enabled = true")
                    .executeUpdate();
        }

        return entityManager.createQuery(
                        "update ProfessionalCategoryEntity c set c.enabled = false " +
                                "where c.enabled = true and c.id not in :ids")
                .setParameter("ids", categories.stream().map(ProfessionalCategory::id).toList())
                .executeUpdate();
    }

    /**
     * Deshabilita los profesionales ausentes y los de categorias deshabilitadas.
     * La categoria ya se actualizo, por eso la subconsulta ve el estado final.
     */
    private int disableProfessionals(List<Professional> professionals) {
        String base = "update ProfessionalEntity p set p.enabled = false where p.enabled = true " +
                "and (p.categoryId in (select c.id from ProfessionalCategoryEntity c where c.enabled = false)";

        if (professionals.isEmpty()) {
            return entityManager.createQuery(base + ")").executeUpdate();
        }

        return entityManager.createQuery(base + " or p.id not in :ids)")
                .setParameter("ids", professionals.stream().map(Professional::id).toList())
                .executeUpdate();
    }

    /**
     * Deshabilita los horarios ausentes y los de profesionales deshabilitados.
     * El profesional ya se actualizo, por eso la subconsulta ve el estado final.
     */
    private int disableSchedules(List<WeeklySchedule> schedules) {
        String base = "update WeeklyScheduleEntity s set s.enabled = false where s.enabled = true " +
                "and (s.professionalId in (select p.id from ProfessionalEntity p where p.enabled = false)";

        if (schedules.isEmpty()) {
            return entityManager.createQuery(base + ")").executeUpdate();
        }

        return entityManager.createQuery(base + " or s.id not in :ids)")
                .setParameter("ids", schedules.stream().map(WeeklySchedule::id).toList())
                .executeUpdate();
    }

    private void updateCatalogVersion(long version, SyncMode mode) {
        CatalogVersionEntity row = entityManager.find(CatalogVersionEntity.class, SINGLETON_ID);
        boolean isNew = row == null;
        if (isNew) {
            row = new CatalogVersionEntity();
            row.setId(SINGLETON_ID);
        }

        row.setLocalVersion(version);
        row.setLastSyncMode(mode.name());
        row.setUpdatedAt(Instant.now());

        if (isNew) {
            // Con ID asignado, persist ejecuta el INSERT en el acto: los campos
            // tienen que estar seteados antes.
            entityManager.persist(row);
        }
    }

    private ProfessionalCategoryEntity toEntity(ProfessionalCategory category) {
        ProfessionalCategoryEntity entity = new ProfessionalCategoryEntity();
        entity.setId(category.id());
        entity.setName(category.name());
        entity.setDescription(category.description());
        entity.setEnabled(category.enabled());
        entity.setCreatedAt(category.createdAt());
        entity.setUpdatedAt(category.updatedAt());
        return entity;
    }

    private ProfessionalEntity toEntity(Professional professional) {
        ProfessionalEntity entity = new ProfessionalEntity();
        entity.setId(professional.id());
        entity.setCategoryId(professional.categoryId());
        entity.setFirstName(professional.firstName());
        entity.setLastName(professional.lastName());
        entity.setEnabled(professional.enabled());
        entity.setCreatedAt(professional.createdAt());
        entity.setUpdatedAt(professional.updatedAt());
        return entity;
    }

    private WeeklyScheduleEntity toEntity(WeeklySchedule schedule) {
        WeeklyScheduleEntity entity = new WeeklyScheduleEntity();
        entity.setId(schedule.id());
        entity.setProfessionalId(schedule.professionalId());
        entity.setDayOfWeek(schedule.dayOfWeek());
        entity.setStartTime(schedule.startTime());
        entity.setEndTime(schedule.endTime());
        entity.setSlotDurationMinutes(schedule.slotDurationMinutes());
        entity.setEnabled(schedule.enabled());
        entity.setCreatedAt(schedule.createdAt());
        entity.setUpdatedAt(schedule.updatedAt());
        return entity;
    }
}
