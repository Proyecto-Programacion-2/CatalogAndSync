package com.programacion2.catalogandsync.sync.application.service;

import com.programacion2.catalogandsync.sync.domain.model.SyncDecision;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
public class SyncPlanner {

    public SyncDecision decide(
            Optional<Long> localVersion,
            long currentVersion,
            long oldestAvailableVersion,
            boolean localStateVerifiable
    ) {
        throw new UnsupportedOperationException();
    }
}
