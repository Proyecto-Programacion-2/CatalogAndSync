package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;


@RequiredArgsConstructor
@Component
public class VersionLedgerAdapter implements VersionLedgerPort {
    private final CatalogVersionReaderPort catalogVersionReader;
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
        throw new UnsupportedOperationException("Cátedra version source not implemented");
    }

    @Override
    public long oldestAvailableVersion() {
        throw new UnsupportedOperationException("Cátedra version source not implemented");
    }
}
