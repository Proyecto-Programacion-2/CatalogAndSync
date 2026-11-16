-- Dedup de eventos Kafka (DISENO_SYNC.md seccion 6, revisado 2026-11-13).
-- eventId es la clave de idempotencia funcional del contrato (IR seccion 15.1).
-- La tabla es una optimizacion: la idempotencia real la garantiza el ledger de
-- versiones, porque un ciclo fallido no avanza local_version.

CREATE TABLE catalog.processed_event (
    event_id        varchar(128) PRIMARY KEY,
    applied_version bigint NOT NULL,
    processed_at    timestamptz NOT NULL DEFAULT now()
);
