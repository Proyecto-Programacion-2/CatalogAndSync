#!/bin/sh
# Base de datos exclusiva para los tests de integracion (perfil `test`).
# Se crea junto con el volumen: mismo motor, datos separados de desarrollo.
# La duena es el rol de la app, que crea el esquema `catalog` y ejecuta Flyway.
# Es idempotente: tambien sirve para aplicarla a mano sobre un volumen existente.
set -eu

TEST_DB_NAME="${TEST_DB_NAME:-catalogandsync_test}"

if [ "$(psql -tAc "SELECT 1 FROM pg_database WHERE datname = '$TEST_DB_NAME'" \
        --username "$POSTGRES_USER" --dbname "$POSTGRES_DB")" = "1" ]; then
    echo "La base $TEST_DB_NAME ya existe; no se recrea."
else
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE DATABASE "$TEST_DB_NAME" OWNER "$CATALOG_DB_USER";
SQL
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$TEST_DB_NAME" <<SQL
CREATE SCHEMA IF NOT EXISTS catalog AUTHORIZATION "$CATALOG_DB_USER";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SQL
