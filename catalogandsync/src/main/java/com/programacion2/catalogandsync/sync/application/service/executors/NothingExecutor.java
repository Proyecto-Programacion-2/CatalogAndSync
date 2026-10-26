package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.NOTHING;

@Slf4j
@Component
public class NothingExecutor {
    public SyncOutcome execute(SyncDecision.DoNothing decision) {
    long localVersion = decision.localVersion();
    log.info("Nothing to sync, local version is {}", localVersion);
    return new SyncOutcome(NOTHING, localVersion, true);
    }
}
