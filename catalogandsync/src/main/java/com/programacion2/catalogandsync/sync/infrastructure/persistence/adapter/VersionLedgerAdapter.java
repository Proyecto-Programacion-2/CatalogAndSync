package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VersionLedgerAdapter implements VersionLedgerPort {
    CatalogVersionReaderPort catalogVersionReader;
    @Override
    public Optional<Long> localVersion() {
        try {
            Optional<Long> localversion = catalogVersionReader.readVersion();
            return localversion;
        }
        catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public long currentVersion() {
        return 0;
    }

    @Override
    public long oldestAvailableVersion() {
        return 0;
    }
}
