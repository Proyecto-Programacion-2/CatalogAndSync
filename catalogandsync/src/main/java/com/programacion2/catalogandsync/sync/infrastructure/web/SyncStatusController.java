package com.programacion2.catalogandsync.sync.infrastructure.web;

import com.programacion2.catalogandsync.sync.domain.ports.in.GetSyncStatusUseCase;
import com.programacion2.catalogandsync.sync.infrastructure.web.dto.SyncStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el estado del ultimo ciclo exitoso (H1.9). Es un endpoint propio de
 * operacion: no forma parte del contrato de la catedra.
 */
@RestController
@RequiredArgsConstructor
public class SyncStatusController {

    private final GetSyncStatusUseCase getSyncStatusUseCase;

    @GetMapping(path = "/api/catalog/sync-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public SyncStatusResponse status() {
        return SyncStatusResponse.from(getSyncStatusUseCase.status());
    }
}
