package com.programacion2.catalogandsync.sync.application.service.executors;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import com.programacion2.catalogandsync.sync.domain.model.SyncOutcome;
import org.springframework.stereotype.Component;

@Component
public class SnapshotExecutor {
    public SyncOutcome execute(SyncDecision.Snapshot decision){
        return null;
    }
}
