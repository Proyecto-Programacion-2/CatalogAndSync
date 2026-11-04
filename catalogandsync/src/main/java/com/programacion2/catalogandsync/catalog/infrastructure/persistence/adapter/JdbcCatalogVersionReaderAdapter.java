package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Lee la version de la copia local desde la fila unica de
 * {@code catalog.catalog_version} (CU_2 §2.1).
 * <p>
 * Devuelve {@link Optional#empty()} en una base nueva (sin fila), que es
 * justamente la senal que el planner interpreta como "hace falta snapshot".
 */
@Component
@RequiredArgsConstructor
public class JdbcCatalogVersionReaderAdapter implements CatalogVersionReaderPort {

    private static final String SELECT_LOCAL_VERSION =
            "SELECT local_version FROM catalog.catalog_version WHERE id = 1";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Long> readVersion() {
        List<Long> versions = jdbcTemplate.query(
                SELECT_LOCAL_VERSION,
                (rs, rowNum) -> rs.getLong("local_version"));
        return versions.stream().findFirst();
    }
}
