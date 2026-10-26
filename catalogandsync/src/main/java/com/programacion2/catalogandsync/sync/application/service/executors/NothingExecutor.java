package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.programacion2.catalogandsync.sync.domain.model.SyncMode.NOTHING;

@Slf4j
@Component
public class NothingExecutor {
    public SyncOutcome execute(SyncDecision.Nothing decision) {
    long currentVersion = decision.version();
    log.info("Nothing changed with this sync");
    return new SyncOutcome(NOTHING,currentVersion, true);
    }
}
