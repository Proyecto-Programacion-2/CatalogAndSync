package com.programacion2.catalogandsync.sync.application.usecases;

import com.programacion2.catalogandsync.catalog.domain.model.CatalogVersion;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogConsistencyPort;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogVersionReaderPort;
import com.programacion2.catalogandsync.sync.application.service.SyncPlanner;
import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.catalog.domain.ports.out.CatalogWritePort;
import com.programacion2.catalogandsync.sync.domain.ports.out.VersionLedgerPort;
import com.programacion2.catalogandsync.sync.infrastructure.persistence.adapter.VersionLedgerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.NOTHING;
import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.SNAPSHOT;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncCatalogUseCaseImpl implements SyncCatalogUseCase {
    private  CatalogWritePort catalogWritePort;
    private  SyncPlanner syncPlanner;
    private VersionLedgerPort versionLedgerPort;
    private CatalogConsistencyPort catalogConsistencyPort;
        @Override
    public SyncOutcome sync() {
            // ledger → planner → fuente de datos → writePort → SyncOutcome
            //Ledger work, its supposed to read versions and get a decision

            Optional<Long> localVersion = versionLedgerPort.localVersion();
            long currentVersion = versionLedgerPort.currentVersion();
            long oldestAvailableVersion = versionLedgerPort.oldestAvailableVersion();
            // here it needs to verify the state of the data
            boolean localStateVerifiable = catalogConsistencyPort.isStateVerifiable(localVersion);
            //planner, returns a decision

            SyncDecision decision = syncPlanner.decide(localVersion,currentVersion,oldestAvailableVersion,localStateVerifiable);


        /* if (mode == NOTHING) {
            log.info("Nothing to sync");
            return new SyncOutcome(mode, appliedVersion, false);
        }

        else if (mode == SNAPSHOT) {
            catalogWritePort.applyVersion();
            //Poner aca un log de que se cambió
        }*/

            return null;
        }


}
