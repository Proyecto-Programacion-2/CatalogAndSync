-- Esquema local de la copia del catalogo (CU_2 §2.1).
-- El esquema `catalog` lo crea el bootstrap de Docker; Flyway es dueno de las tablas.
-- Las PK son los IDs de la catedra: no hay secuencias locales.

CREATE TYPE catalog.day_of_week AS ENUM (
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY'
);

CREATE TABLE catalog.professional_category (
    id          bigint PRIMARY KEY,
    name        varchar(255) NOT NULL,
    description varchar(255),
    enabled     boolean NOT NULL,
    created_at  timestamptz,
    updated_at  timestamptz
);

CREATE TABLE catalog.professional (
    id          bigint PRIMARY KEY,
    category_id bigint NOT NULL REFERENCES catalog.professional_category (id),
    first_name  varchar(255) NOT NULL,
    last_name   varchar(255) NOT NULL,
    enabled     boolean NOT NULL,
    created_at  timestamptz,
    updated_at  timestamptz
);

CREATE TABLE catalog.weekly_schedule (
    id                    bigint PRIMARY KEY,
    professional_id       bigint NOT NULL REFERENCES catalog.professional (id),
    day_of_week           catalog.day_of_week NOT NULL,
    start_time            time NOT NULL,
    end_time              time NOT NULL,
    slot_duration_minutes integer NOT NULL,
    enabled               boolean NOT NULL,
    created_at            timestamptz NOT NULL,
    updated_at            timestamptz
);

CREATE INDEX idx_weekly_schedule_professional_day_enabled
    ON catalog.weekly_schedule (professional_id, day_of_week, enabled);

-- Fila unica: la version local solo avanza cuando la unidad de trabajo termino.
CREATE TABLE catalog.catalog_version (
    id             smallint PRIMARY KEY DEFAULT 1,
    local_version  bigint NOT NULL,
    last_sync_mode varchar(16) NOT NULL,
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT catalog_version_singleton CHECK (id = 1),
    CONSTRAINT catalog_version_mode CHECK (last_sync_mode IN ('SNAPSHOT', 'INCREMENTAL'))
);
