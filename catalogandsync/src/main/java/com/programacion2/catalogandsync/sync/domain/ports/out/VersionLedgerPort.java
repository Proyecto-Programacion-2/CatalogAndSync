package com.programacion2.catalogandsync.sync.domain.ports.out;

import java.util.Optional;

public interface VersionLedgerPort {

    Optional<Long> localVersion();

    long currentVersion();

    long oldestAvailableVersion();
}
