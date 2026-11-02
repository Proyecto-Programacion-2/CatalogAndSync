#!/bin/sh
# Bootstrap de roles/esquema. Corre UNA SOLA VEZ, cuando el volumen se inicializa.
# El rol de la app NO es superusuario y NO puede crear otros esquemas: solo es dueño de `catalog`.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE "$CATALOG_DB_USER" LOGIN PASSWORD '$CATALOG_DB_PASSWORD'
    NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE SCHEMA catalog AUTHORIZATION "$CATALOG_DB_USER";
SQL
