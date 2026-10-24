package com.programacion2.catalogandsync.catalog.domain.ports.out;

import java.util.Optional;

public interface CatalogConsistencyPort {
    boolean isStateVerifiable (Optional<Long> version);
}
