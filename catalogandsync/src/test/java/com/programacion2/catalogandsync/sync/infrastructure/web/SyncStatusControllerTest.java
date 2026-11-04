package com.programacion2.catalogandsync.sync.infrastructure.web;

import com.programacion2.catalogandsync.sync.domain.model.SyncMode;
import com.programacion2.catalogandsync.sync.domain.model.SyncStatus;
import com.programacion2.catalogandsync.sync.domain.ports.in.GetSyncStatusUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SyncStatusControllerTest {

    private static final Instant LAST_SYNC = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void returnsTheRecordedStatus() throws Exception {
        MockMvc mockMvc = mockMvc(new SyncStatus(10L, 20L, 5L, SyncMode.SNAPSHOT, LAST_SYNC, false));

        mockMvc.perform(get("/api/catalog/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(10))
                .andExpect(jsonPath("$.currentVersion").value(20))
                .andExpect(jsonPath("$.oldestAvailableVersion").value(5))
                .andExpect(jsonPath("$.lastSyncMode").value("SNAPSHOT"))
                .andExpect(jsonPath("$.lastSyncAt").value(LAST_SYNC.toString()))
                .andExpect(jsonPath("$.stale").value(false));
    }

    @Test
    void returnsNullsWhenNeverSynced() throws Exception {
        MockMvc mockMvc = mockMvc(new SyncStatus(null, null, null, null, null, true));

        mockMvc.perform(get("/api/catalog/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localVersion").value(nullValue()))
                .andExpect(jsonPath("$.currentVersion").value(nullValue()))
                .andExpect(jsonPath("$.oldestAvailableVersion").value(nullValue()))
                .andExpect(jsonPath("$.lastSyncMode").value(nullValue()))
                .andExpect(jsonPath("$.lastSyncAt").value(nullValue()))
                .andExpect(jsonPath("$.stale").value(true));
    }

    private static MockMvc mockMvc(SyncStatus status) {
        GetSyncStatusUseCase useCase = () -> status;
        return MockMvcBuilders.standaloneSetup(new SyncStatusController(useCase)).build();
    }
}
