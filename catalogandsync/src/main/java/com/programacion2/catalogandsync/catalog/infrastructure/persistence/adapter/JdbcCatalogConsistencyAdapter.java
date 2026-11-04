package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogConsistencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementacion real de {@link CatalogConsistencyPort} sobre PostgreSQL
 * (DISENO_SYNC §3).
 * <p>
 * Checks modelados:
 * <ul>
 *   <li><b>1. Fila de version existe:</b> {@code localVersion} vacio implica
 *       base nueva o lector sin fila.</li>
 *   <li><b>4. Consistencia de las 3 colecciones:</b> o las tres tienen filas o
 *       las tres estan vacias. Un catalogo legitimamente vacio (version
 *       commiteada y cero filas) es verificable; una mezcla indica un apply
 *       interrumpido.</li>
 * </ul>
 * No modelados todavia, por decision de diseno: el <b>2</b> (ultimo sync no
 * fallo) y el <b>5</b> (sanidad referencial) son nice-to-have y no bloquean. El
 * <b>3</b> (atomicidad del ultimo apply) lo garantiza estructuralmente
 * {@code JpaCatalogWriteAdapter}, que escribe datos y version en una sola
 * transaccion; no se re-verifica.
 * <p>
 * Fail-safe (DECIDIDO 2026-11-02): cualquier error de acceso a datos al
 * consultar la base se traduce en {@code false} (log + snapshot), nunca se
 * propaga al caso de uso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcCatalogConsistencyAdapter implements CatalogConsistencyPort {

    private static final String COLLECTION_PRESENCE = """
            SELECT EXISTS (SELECT 1 FROM catalog.professional_category) AS categories,
                   EXISTS (SELECT 1 FROM catalog.professional)         AS professionals,
                   EXISTS (SELECT 1 FROM catalog.weekly_schedule)      AS schedules
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isStateVerifiable(Optional<Long> version) {
        if (version == null || version.isEmpty()) {
            return false;
        }

        try {
            Boolean consistent = jdbcTemplate.queryForObject(COLLECTION_PRESENCE, (rs, rowNum) -> {
                boolean categories = rs.getBoolean("categories");
                boolean professionals = rs.getBoolean("professionals");
                boolean schedules = rs.getBoolean("schedules");
                return (categories && professionals && schedules)
                        || (!categories && !professionals && !schedules);
            });
            return Boolean.TRUE.equals(consistent);
        } catch (DataAccessException ex) {
            log.warn("Could not verify local catalog state; forcing snapshot", ex);
            return false;
        }
    }
}
