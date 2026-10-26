package com.programacion2.catalogandsync.catalog.domain.ports.out;

import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public interface CatalogVersionReaderPort {
    Optional<Long> readVersion();
}
