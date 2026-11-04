package com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter;

import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import com.programacion2.catalogandsync.sync.application.exception.VersionSourceException;
import com.programacion2.catalogandsync.sync.domain.ports.out.RemoteVersionPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica la composicion local + remoto del ledger usando dobles de los
 * puertos propios. No emula a la catedra: solo comprueba el cableado.
 */
class VersionLedgerAdapterTest {

    @Test
    void localVersionComesFromTheLocalReader() {
        VersionLedgerAdapter adapter = new VersionLedgerAdapter(() -> Optional.of(3L), remote(7, 4));

        assertEquals(Optional.of(3L), adapter.localVersion());
    }

    @Test
    void localVersionIsEmptyWhenTheReaderFails() {
        CatalogVersionReaderPort failingReader = () -> {
            throw new IllegalStateException("database down");
        };
        VersionLedgerAdapter adapter = new VersionLedgerAdapter(failingReader, remote(7, 4));

        assertTrue(adapter.localVersion().isEmpty());
    }

    @Test
    void remoteVersionsComeFromTheCatedraPort() {
        VersionLedgerAdapter adapter = new VersionLedgerAdapter(Optional::empty, remote(7, 4));

        assertEquals(7L, adapter.currentVersion());
        assertEquals(4L, adapter.oldestAvailableVersion());
    }

    @Test
    void remoteFailuresPropagateInsteadOfBecomingFakeVersions() {
        RemoteVersionPort failingRemote = new RemoteVersionPort() {
            @Override
            public long currentVersion() {
                throw new VersionSourceException("redis down");
            }

            @Override
            public long oldestAvailableVersion() {
                throw new VersionSourceException("redis down");
            }
        };
        VersionLedgerAdapter adapter = new VersionLedgerAdapter(Optional::empty, failingRemote);

        assertThrows(VersionSourceException.class, adapter::currentVersion);
        assertThrows(VersionSourceException.class, adapter::oldestAvailableVersion);
    }

    private static RemoteVersionPort remote(long current, long oldest) {
        return new RemoteVersionPort() {
            @Override
            public long currentVersion() {
                return current;
            }

            @Override
            public long oldestAvailableVersion() {
                return oldest;
            }
        };
    }
}
