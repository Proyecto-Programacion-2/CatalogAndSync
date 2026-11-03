package com.programacion2.catalogandsync.catalog.domain.ports.out;

import java.util.Optional;

public interface CatalogVersionReaderPort {
    Optional<Long> readVersion();
}
