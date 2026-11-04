package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Fail-safe de DISENO_SYNC §3: un error de I/O al verificar no se propaga, se
 * traduce en {@code false} para que el planner tome snapshot.
 */
@ExtendWith(MockitoExtension.class)
class JdbcCatalogConsistencyAdapterFailSafeTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ioFailureIsTreatedAsNotVerifiable() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        JdbcCatalogConsistencyAdapter adapter = new JdbcCatalogConsistencyAdapter(jdbcTemplate);

        assertFalse(adapter.isStateVerifiable(Optional.of(7L)));
    }
}
