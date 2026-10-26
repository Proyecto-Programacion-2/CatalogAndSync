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
        /* the first if [if (!localStateVerifiable)] MUST come first, the reasoning is:
        if the DB is broken/down then: (localStateVerifiable = False and localVersion.isEmpty() = True)
        that makes it so that the reasoning for the snapshot was always going to be empty catalog
        and not local state broken.
         */
        if (!localStateVerifiable) {
            return  new SyncDecision.TakeSnapshot("Local_State_Not_Verifiable");
        }
        if (localVersion.isEmpty()) {
            return  new SyncDecision.TakeSnapshot("Empty_Local_Catalog");
        }

        long local = localVersion.get();
        if (local < oldestAvailableVersion) {
            return new SyncDecision.TakeSnapshot("Local_Version_Out_Of_Window");
        }
        if (local >= currentVersion) {
            return new SyncDecision.DoNothing(local);
        }
        return new SyncDecision.ApplyIncremental(local +1, currentVersion);

    }
}
