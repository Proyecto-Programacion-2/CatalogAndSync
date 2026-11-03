#!/bin/sh
# Extensiones de PostgreSQL que necesita la copia local del catalogo.
# Las crea el rol administrador: el rol de la app NO tiene CREATE sobre la base
# y por eso no puede ejecutar CREATE EXTENSION por si mismo.
# Igual que 01-catalog-role.sh, corre UNA SOLA VEZ al inicializar el volumen.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
-- Indice trigram para la busqueda por fragmento de nombre (Fase 2.5).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SQL
