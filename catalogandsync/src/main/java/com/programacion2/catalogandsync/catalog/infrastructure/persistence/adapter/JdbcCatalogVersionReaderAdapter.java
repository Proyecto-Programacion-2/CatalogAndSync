package com.programacion2.catalogandsync.catalog.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;

import java.util.Optional;

public class JdbcCatalogVersionReaderAdapter implements CatalogVersionReaderPort {

    @Override
    public Optional<Long> readVersion() {
        return Optional.empty();
    }
}
