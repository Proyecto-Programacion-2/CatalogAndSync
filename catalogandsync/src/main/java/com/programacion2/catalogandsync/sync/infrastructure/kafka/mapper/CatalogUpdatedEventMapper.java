package com.programacion2.catalogandsync.sync.infrastructure.kafka.mapper;

import com.programacion2.catalogandsync.sync.application.exception.CatedraContractException;
import com.programacion2.catalogandsync.sync.domain.model.CatalogUpdatedNotification;
import com.programacion2.catalogandsync.sync.infrastructure.kafka.dto.CatalogUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Valida el evento {@code CatalogUpdated} de Kafka (IR seccion 15.3) y lo
 * traduce a {@link CatalogUpdatedNotification}.
 * <p>
 * Reparto de responsabilidades, igual que en la ACL REST/Redis:
 * <ul>
 *   <li>este mapper valida el contrato de cable: JSON valido, {@code eventType}
 *       exacto, {@code schemaVersion = 1}, {@code eventId} presente y dentro de
 *       la columna ({@code varchar(128)}), {@code newVersion} positivo y la
 *       message key igual a la version;</li>
 *   <li>el record de dominio valida sus invariantes y este mapper traduce la
 *       falla a {@link CatedraContractException} con el contexto del canal.</li>
 * </ul>
 * Un evento invalido no tiene efecto funcional: el estado no depende de su
 * payload, solo de Redis y del ledger local.
 */
@Component
@RequiredArgsConstructor
public class CatalogUpdatedEventMapper {

    static final String EVENT_TYPE = "CatalogUpdated";
    static final int SUPPORTED_SCHEMA_VERSION = 1;
    static final int MAX_EVENT_ID_LENGTH = 128;

    private final JsonMapper jsonMapper;

    public CatalogUpdatedNotification toNotification(String key, String payload, String context) {
        if (payload == null || payload.isBlank()) {
            throw new CatedraContractException(context + " has an empty payload");
        }

        CatalogUpdatedEvent event;
        try {
            event = jsonMapper.readValue(payload, CatalogUpdatedEvent.class);
        } catch (JacksonException ex) {
            throw new CatedraContractException(context + " is not valid JSON: " + ex.getOriginalMessage(), ex);
        }
        if (event == null) {
            throw new CatedraContractException(context + " has an empty payload");
        }

        if (!EVENT_TYPE.equals(event.eventType())) {
            throw new CatedraContractException(
                    context + " has eventType '" + event.eventType() + "' but expected '" + EVENT_TYPE + "'");
        }
        if (event.schemaVersion() == null || event.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new CatedraContractException(context + " has schemaVersion '" + event.schemaVersion()
                    + "' but expected " + SUPPORTED_SCHEMA_VERSION);
        }
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new CatedraContractException(context + " field 'eventId' is missing");
        }
        if (event.eventId().length() > MAX_EVENT_ID_LENGTH) {
            throw new CatedraContractException(context + " eventId is longer than "
                    + MAX_EVENT_ID_LENGTH + " characters");
        }
        if (event.newVersion() == null || event.newVersion() <= 0) {
            throw new CatedraContractException(
                    context + " has an invalid newVersion: " + event.newVersion());
        }

        long keyVersion = parseKey(key, context);
        if (keyVersion != event.newVersion()) {
            throw new CatedraContractException(context + " message key is " + keyVersion
                    + " but newVersion is " + event.newVersion());
        }

        try {
            return new CatalogUpdatedNotification(
                    event.eventId(), event.newVersion(), event.occurredAt(), event.source());
        } catch (IllegalArgumentException ex) {
            throw new CatedraContractException(context + " is invalid: " + ex.getMessage(), ex);
        }
    }

    private static long parseKey(String key, String context) {
        if (key == null || key.isBlank()) {
            throw new CatedraContractException(context + " has no message key");
        }
        try {
            return Long.parseLong(key);
        } catch (NumberFormatException ex) {
            throw new CatedraContractException(
                    context + " message key '" + key + "' is not a version", ex);
        }
    }
}
