# MAP.md — CatalogAndSync

Plan de desarrollo de ESTE repositorio, autocontenido. El mapa global del proyecto (las 8 fases y su orden) vive en `MAP.md` en la raiz del workspace y no se propaga. Fuentes de verdad: `PROJECT_STATEMENT-v1.md`, `INTEGRATION_REFERENCE-v2.md`, `ARQUITECTURA.md`, `Docs/CU_2.md`.

## Dependencias de otros repos
- **Entradas necesarias para avanzar**: ningun otro repo te bloquea para arrancar. Necesitas la cuenta tecnica de la catedra (Fase 0) y, desde el hito de esquema, una base de datos PostgreSQL.
- **Salidas que otros consumen de vos**: TurnosYReservas consume la agenda vigente del profesional (contrato interno, aca en Fase 3). KMPapp consume registro/login y busquedas (Fase 2).

## Decisiones abiertas (no asumir; consultar al usuario)
| Decision | Bloquea | Referencia |
| --- | --- | --- |
| Herramienta de migraciones (Flyway/Liquibase) | H1.2 (esquema) | PS §3 |
| Regla de baja fisica: IDs que no vienen en el snapshot | H1.4 | IR §7 |
| Donde se persiste el `eventId` para dedup (tabla propia vs `alumnos:{gid}:*`) | H1.7 | IR §15.3, §18.1 |
| `day_of_week` enum vs `varchar(9)`; criterio e indice de busqueda por nombre | Fase 2 | CU_2 §2.1 |
| Que hacer con `CatalogandsyncApplicationTests.contextLoads()` (rompe el build verde) | H0.6 | — |
| Timeouts, backoff y limites de reintentos de la sync | H1.9 | ARQUITECTURA §4 |
| Autenticacion interna hacia TurnosYReservas (JWT de usuario vs JWT tecnico) | Fase 3 | PS §9 |

## Fase 0 — Cimientos

| # | Hito | Criterio de salida |
| --- | --- | --- |
| 0.1 | Cuenta tecnica de catedra: `POST /api/student/register` a mano, una vez. Guardar `id_token` + bloque `integration` FUERA del repo. Si se pierde el JWT: nueva cuenta (IR §5.4) | `provisioningStatus: PROVISIONED` |
| 0.2 | Decidir herramienta de migraciones | Decidida y registrada en `ARQUITECTURA.md` |
| 0.3 | Higiene del repo: commitear el scaffold de JHipster, nunca `catalogandsync.zip` | `git status` limpio |
| 0.4 | Docker Compose con PostgreSQL (DECIDIDO, cuando la sync lo necesite). Antes: entorno — grupo `docker` (`sudo usermod -aG docker $USER` + re-login), plugin Compose v2 (hoy solo `docker-compose` v1). Compose = SOLO PostgreSQL (IR §3) | Compose levanta; usuario y esquema `catalog` creados |
| 0.5 | Config externalizada: `application.yaml` solo `${...}`; cero secretos | Arranca sin secretos logueados |
| 0.6 | Build verde: resolver `contextLoads()` sin datasource | `./mvnw test` pasa |

Trampa: el cliente de catedra (REST/Redis/Kafka) NO se comparte con TurnosYReservas (repos separados, PS §13.1). Se escribe en cada backend.

## Fase 1 — Sincronizacion (el foco)

Copia local SIEMPRE consistente con la version de la catedra, por snapshot, incremental y notificacion, capaz de ponerse al dia aunque pierda notificaciones.

| # | Hito | Criterio de salida |
| --- | --- | --- |
| 1.1 | Logica pura `decidirSync(local, current, oldest)` → Snapshot / Incremental(desde V+1) / Nada. Casos: BD vacia → snapshot; `V < oldest` → snapshot (IR §18.2); `V >= current` → nada; `V < current` → incremental; estado no verificable → snapshot | Tests unitarios de todos los casos |
| 1.2 | Esquema local: `professional_category`, `professional`, `weekly_schedule`, `catalog_version` (CU_2 §2.1). PK = ID de catedra. Fila unica para `catalog_version`. Indice `(professional_id, day_of_week, enabled)` | Migraciones aplicadas sobre Postgres; esquema inspeccionado |
| 1.3 | Cliente de catedra por puertos de salida: `GET /api/synchronization/snapshot` con JWT tecnico; deserializar las 3 colecciones | Snapshot crudo guardado; JSON deserializa sin perdidas |
| 1.4 | Aplicar snapshot consistente: 3 colecciones + `snapshotVersion` en UNA transaccion (IR §7). Upsert por ID; `enabled=false` = baja logica. Idempotente | BD = snapshot; `local_version = snapshotVersion`; `last_sync_mode = SNAPSHOT` |
| 1.5 | Versionado Redis: `catedra:sync:current-version` + `oldest-available-version` + `metadata`; solo lectura de `catedra:sync:*` | Test de discontinuidad con (3, 7, 4) |
| 1.6 | Incremental: `changes:{version}` → IDs afectados; resolver estado vigente en los 3 hashes; aplicar V+1..current en orden; version avanza solo si la unidad termino (IR §14.5). `changes:{v}` faltante → snapshot | De V a V+n en orden; `local_version = current` |
| 1.7 | Consumidor `CatalogUpdated` (topic `catedra.catalog.{gid}`, key = `newVersion`). Kafka = notificacion, no fuente. Dedup por `eventId`. Offset DESPUES de persistir (IR §16): `enable-auto-commit=false` + ack manual | Version nueva aplicada; mismo `eventId` repetido no cambia nada |
| 1.8 | Reconciliacion periodica: `local_version` vs `current-version` → aplicar atraso | Sin consumo Kafka, la tarea pone al dia |
| 1.9 | Robustez: timeouts, reintentos limitados y observables (IR §18.4); reconciliar por version antes de repetir; lectura local no se cae si catedra no responde (PS §8); opcional `alumnos:{gid}:*` para diagnostico (IR §14.6) | Tests de logica pura + evidencia manual; decisiones en `ARQUITECTURA.md` |

Trampas:
- `changes:{v}` NO tiene TTL contractual: no asumir que existe; si falta, snapshot, no error.
- Message key = `newVersion`, no `eventId` → dos eventos de la misma version caen en la misma particion en orden.
- El consumidor NO escribe en `catedra:sync:*`; el ACL solo permite escribir `alumnos:{gid}:*`.
- Snapshot e incremental usan EL MISMO codigo de escritura (misma transaccion de version); si divergen, el catalogo no se reconstruye.

Evidencias PS §11: inicializacion desde snapshot · actualizacion incremental · recuperacion por snapshot tras discontinuidad.

## Fase 2 — Cuenta del usuario final y busqueda

- 2.1 Tabla de usuarios compatible JHipster (CU_1 §1.1) + registro con password hasheada y salada + JWT. DECIDIDO: vive aca.
- 2.2 Autorizacion: escritura del catalogo SOLO con JWT tecnico; lectura por usuarios KMP (CU_2 §2.1).
- 2.3 Filtros sobre la copia local: categoria, nombre, `enabled`.
- 2.4 Filtro disponibilidad: via contrato interno a TurnosYReservas, nunca a la catedra.
- 2.5 Decidir indice de nombre y `day_of_week`.
Criterio: un usuario registrado filtra por los 4 y el resultado sale de la BD local.

## Fase 3 — Contrato interno hacia TurnosYReservas

- 3.1 Endpoint de agenda vigente del profesional.
- 3.2 DTOs, errores y autenticacion interna (decision abierta).
- 3.3 Definir vigencia de la respuesta (que pasa si el catalogo esta atrasado).