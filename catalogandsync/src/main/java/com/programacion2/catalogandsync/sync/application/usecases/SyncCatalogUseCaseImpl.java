package com.programacion2.catalogandsync.sync.application.usecases;

import com.programacion2.catalogandsync.sync.application.service.SyncPlanner;
import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import com.programacion2.catalogandsync.sync.domain.ports.in.SyncCatalogUseCase;
import com.programacion2.catalogandsync.sync.domain.ports.out.CatalogWritePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.NOTHING;
import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.SNAPSHOT;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncCatalogUseCaseImpl implements SyncCatalogUseCase {
    private CatalogWritePort catalogWritePort;
        @Override
    public SyncOutcome sync() {
            // ledger → planner → fuente de datos → writePort → SyncOutcome

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
