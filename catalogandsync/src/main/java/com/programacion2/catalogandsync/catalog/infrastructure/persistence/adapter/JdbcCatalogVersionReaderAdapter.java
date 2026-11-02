package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JdbcCatalogVersionReaderAdapter implements CatalogVersionReaderPort {

    @Override
    public Optional<Long> readVersion() {
        return Optional.empty();
    }
}
