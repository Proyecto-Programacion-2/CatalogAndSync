package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.RemoteVersionPort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Compone las dos fuentes de version: la copia local (PostgreSQL) y la catedra
 * (Redis). Mantiene el ledger como un unico puerto para el caso de uso.
 */
@RequiredArgsConstructor
@Component
public class VersionLedgerAdapter implements VersionLedgerPort {

    private final CatalogVersionReaderPort catalogVersionReader;
    private final RemoteVersionPort remoteVersionPort;

    @Override
    public Optional<Long> localVersion() {
        try {
            return catalogVersionReader.readVersion();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public long currentVersion() {
        return remoteVersionPort.currentVersion();
    }

    @Override
    public long oldestAvailableVersion() {
        return remoteVersionPort.oldestAvailableVersion();
    }
}
