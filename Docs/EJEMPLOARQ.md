# Ejemplo de arquitectura hexagonal — CatalogAndSync

> Documento de referencia y aprendizaje. NO es la fuente de verdad de la arquitectura: la fuente canónica es `ARQUITECTURA.md`. Este ejemplo ilustra cómo organizar el servicio en dos bounded contexts (`catalog` y `sync`) dentro de un mismo deployable.

## 0. En qué se basa

- Un solo servicio backend (cumple la regla de "exactamente dos servicios" del enunciado, PS §3).
- Contextos separados por responsabilidad: `catalog` (lectura/búsqueda y contrato con Turnos) y `sync` (snapshot + incremental + reconstrucción).
- La atomicidad de versión (IR §7, §14.5) se logra porque ambos contextos comparten el puerto de persistencia: `sync` escribe el catálogo a través del mismo adaptador JPA que usa `catalog`.
- Tablas del CU_2: `professional_category`, `professional`, `weekly_schedule`, `catalog_version`.

## 1. Árbol de carpetas

```
CatalogAndSync/
├── src/main/java/com/proyecto/catalogosync/
│
│   ├── catalog/                         # CONTEXTO: catálogo (lectura, búsqueda, contrato con Turnos)
│   │   ├── domain/                      # núcleo puro: sin Spring, sin JPA
│   │   │   ├── model/                          # ProfessionalCategory, Professional, WeeklySchedule, CatalogVersion
│   │   │   ├── port/
│   │   │   │   ├── in/                          # casos de uso (interfaces que expone el núcleo)
│   │   │   │   │   ├── SearchCatalogUseCase.java            # filtros KMP (categoría, nombre, enabled)
│   │   │   │   │   └── GetVigentScheduleUseCase.java        # agenda vigente → Turnos (PS §4.2)
│   │   │   │   └── out/                         # lo que el núcleo necesita del "mundo exterior"
│   │   │   │       ├── CatalogReadPort.java
│   │   │   │       └── AvailabilityLookupPort.java          # resuelve el filtro "disponibilidad" (→ Turnos)
│   │   │   └── service/
│   │   │       ├── SearchCatalogService.java
│   │   │       └── GetVigentScheduleService.java
│   │   └── infrastructure/              # adaptadores (dependencias apuntan hacia adentro)
│   │       ├── in/
│   │       │   └── web/                         # @RestController
│   │       │       ├── CatalogSearchController.java     # hacia KMP (JWT de usuario, solo lectura)
│   │       │       └── ScheduleController.java          # contrato interno hacia Turnos (agenda vigente)
│   │       └── out/
│   │           ├── persistence/                  # JPA repositories → implementan CatalogReadPort/WritePort
│   │           │   ├── ProfessionalCategoryJpaRepository.java
│   │           │   ├── ProfessionalJpaRepository.java
│   │           │   ├── WeeklyScheduleJpaRepository.java
│   │           │   └── CatalogVersionRepository.java
│   │           └── turnos/                       # cliente HTTP → implementa AvailabilityLookupPort
│   │
│   ├── sync/                            # CONTEXTO: sincronización (snapshot + incremental + reconstrucción)
│   │   ├── domain/
│   │   │   ├── model/                          # CatalogUpdateNotification, VersionChange, SyncResult
│   │   │   ├── port/
│   │   │   │   ├── in/
│   │   │   │   │   └── SyncOrchestrator.java            # applyVersion(), rebuildFromSnapshot()
│   │   │   │   └── out/
│   │   │   │       ├── CatalogDataSourcePort.java       # snapshot REST + cambios incrementales (fuente)
│   │   │   │       ├── VersionLedgerPort.java           # lee/compara versiones (local, current, oldest)
│   │   │   │       └── CatalogWritePort.java            # ⚠ reusa adaptador de catalog.out.persistence
│   │   │   └── service/
│   │   │       ├── IncrementalSyncService.java          # Kafka→Redis, aplica en orden
│   │   │       ├── SnapshotSyncService.java             # reconstrucción
│   │   │       └── DiscontinuityDetector.java           # version < oldest-available → snapshot (IR §14.2)
│   │   └── infrastructure/
│   │       ├── in/
│   │       │   └── kafka/
│   │       │       └── CatalogUpdatedConsumer.java      # @KafkaListener, dedup por eventId (IR §15.3, §18.1)
│   │       └── out/
│   │           ├── rest/
│   │           │   └── CatedraRestClient.java           # GET snapshot (JWT técnico) → CatalogDataSourcePort
│   │           ├── redis/
│   │           │   └── CatedraRedisSource.java          # versiones + changes:{v} + hashes → CatalogDataSourcePort
│   │           └── scheduler/
│   │               └── SyncReconciliationTask.java      # autofix: detecta notificaciones perdidas comparando versiones (IR §16)
│   │
│   └── shared/                           # transversal (no es un tercer dominio)
│       ├── security/                     # filtros JWT (usuario KMP y técnico), nunca secretos en código
│       ├── error/                        # traduce a problem+json estándar
│       └── config/                       # DataSource, Kafka listener factory, Redis
│
├── src/test/java/com/proyecto/catalogosync/
│   ├── catalog/                          # unit (dominio) + integration (JPA sobre Postgres)
│   └── sync/                             # sync con stub de catedra (Docker Compose)
└── docker-compose.yml                    # postgres + kafka + redis (+ stub catedra opcional)
```

## 2. Reglas que transmite el árbol

1. **Las dependencias apuntan hacia adentro.** `infrastructure` conoce a `domain`, nunca al revés. Los `@Controller`, `@KafkaListener` y `@Repository` son adaptadores, no lógica de negocio.
2. **Los puertos *in* son los casos de uso** (interfaces que el núcleo expone); **los puertos *out*** son lo que el núcleo necesita del exterior. Spring los conecta por inyección de dependencias.
3. **`catalog` y `sync` comparten el puerto `CatalogWritePort`**, cuyo adaptador vive una sola vez en `catalog.out.persistence`. `sync` escribe a través de él, y eso hace posible que **versión + datos avancen en la misma transacción** (IR §7, §14.5).
4. **`shared/` solo contiene infra transversal** (security, error, config): no alberga dominios ni lógica de negocio.

## 3. Trazabilidad

| Pieza | Fuente |
| --- | --- |
| Tres entidades + versión local | CU_2 §2.1; IR §7, §14.2, §14.4 |
| Filtros (categoría, nombre, enabled) | CU_2 §2.2; PS §4.1, §6 |
| Filtro disponibilidad vía contrato interno | CU_2 §2.2; PS §4.2 |
| Sincronización completa/incremental/reconstrucción | PS §6; IR §16 |
| Deduplicación por `eventId` | IR §15.1, §18.1 |
| Atomicidad de versión | IR §7, §14.5 |