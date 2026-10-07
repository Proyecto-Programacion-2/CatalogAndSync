# Responsabilidades del slice `sync` — CatalogAndSync

> Documento de diseño. Es el **insumo previo** al modelado de clases del bounded context `sync`: primero se acuerda *qué tiene que hacer* (este documento), después *cómo se implementa* (clases, puertos, adaptadores). No es código ni fuente de verdad de decisiones: las decisiones vigentes viven en `ARQUITECTURA.md`.

Fuentes de este documento: `PROJECT_STATEMENT-v1.md` (PS) §4.1, §5, §6.1, §6.2, §8, §10, §11; `INTEGRATION_REFERENCE-v2.md` (IR) §3, §4, §5.1-5.4, §7, §14.1-14.6, §15.1, §15.3, §16, §18.1, §18.2, §18.4. Apoyo: `CatalogAndSync/Docs/CU_2.md` (esquema local), `MAP.md` Fase 1.

## 0. Para qué sirve y cómo se usó

El slice `sync` mantiene la copia local del catálogo siempre consistente con la versión de la cátedra: por snapshot (REST), por incremental (Kafka notifica, Redis aporta los datos) y reconstruyéndose cuando no puede continuar.

Se agrupó el trabajo por **ciclo de vida** de una sincronización, no por sus modos. Consecuencia central: *snapshot*, *incremental* y *reconstrucción* **no son clases**, son **modos resultantes** de combinar las mismas responsabilidades. En particular, **la escritura es una sola** (R4): si snapshot e incremental usaran caminos de escritura distintos, el catálogo podría divergir y no reconstruirse (trampa `MAP.md` Fase 1).

Cada item del inventario (§1) cae en **exactamente una** responsabilidad (§2). Las decisiones que aún no están tomadas quedan **marcadas como pendientes** dentro de la responsabilidad que afectan (§3), sin resolverlas acá.

---

## 1. Inventario trazado ("qué tiene que hacer sync")

Agrupado por origen. `⟂` = derivado de dos fuentes (no literal en una sola). `≈` = mismo requisito expresado en otra fuente, dejado sin fusionar.

### A — Acceso a la infraestructura de la cátedra
- **A1.** Autenticar los llamados REST a cátedra con `Authorization: Bearer <jwt-tecnico>`; el secreto vive en variable de entorno, jamás en código ni logs. Si el JWT se pierde/expone: cuenta técnica nueva (IR §4, §5.4).
- **A2.** Rotar el JWT (dura 1 año) con `POST /api/authenticate` + `rememberMe: true` y el password de la cuenta técnica (IR §5.2).
- **A3.** Conectar a Redis con `redisHost/Port/Username/Password` del bloque `integration` y respetar la ACL: lectura en `catedra:sync:*`, escritura solo en `alumnos:{groupId}:*` (IR §14.1).
- **A4.** Tomar topics, consumer group y bootstrap servers de `integration`, externalizados en el entorno (IR §3, §5.3). ⟂ Hoy viven en el `.env` (fase 0.1).

### B — Sincronización completa (snapshot)
- **B1.** Obtener `GET /api/synchronization/snapshot` cuando la BD local está vacía, hay que reconstruirla o no se puede continuar de forma segura desde la versión actual (PS §5.4, §6.1).
- **B2.** Aplicar el snapshot de forma consistente: las 3 colecciones (`professionalCategories`, `professionals`, `weeklySchedules`) juntas (IR §7).
- **B3.** Registrar `snapshotVersion` como versión local solo después de aplicar correctamente las 3 colecciones (IR §7, PS §6.1).
- **B4.** Conservar como versión local exactamente la versión que corresponde a los datos almacenados (PS §4.1, §6.1).
- **B5.** Actualizar por ID de cátedra (PK = ID de cátedra); los registros con `enabled=false` entran como baja lógica, no se borran (IR §7; CU_2 §2.1).

### C — Sincronización incremental (Kafka → Redis → BD)
- **C1.** Recibir `CatalogUpdated` del topic `catedra.catalog.{groupId}`; el evento **notifica, no es fuente**: los datos se leen de Redis o de snapshot (IR §15.3).
- **C2.** Ignorar campos desconocidos compatibles con `schemaVersion: 1` (IR §15.1).
- **C3.** Deduplicar por `eventId`: procesar dos veces el mismo evento no altera el resultado final (IR §15.1, §18.1; PS §6.2).
- **C4.** Leer versiones y metadata en Redis: `current-version`, `oldest-available-version`, `metadata` (IR §14.2, §14.3).
- **C5.** Leer `changes:{version}` → IDs afectados por colección (no entidades completas) (IR §14.5).
- **C6.** Resolver el estado vigente de cada ID en los hashes `professional-categories` / `professionals` / `weekly-schedules` (IR §14.4, §14.5).
- **C7.** Aplicar en orden V+1…current; la versión local solo avanza cuando la unidad de trabajo de esa versión terminó correctamente (PS §6.2, IR §14.5).
- **C8.** Commitear el offset de Kafka **después** de persistir: `enable-auto-commit=false` + ack manual (IR §16).
- **C9.** ≈B2/B3: la escritura de una versión incremental también es atómica (datos + versión juntos) (IR §14.5).

### D — Detección de discontinuidad
- **D1.** Si `local_version < oldest-available-version` → faltan incrementales → snapshot (IR §14.2, §18.2).
- **D2.** Si el estado local no puede verificarse o no se puede continuar de forma segura → snapshot (PS §6.1, §6.2).
- **D3.** ≈D1/D2: reconstruir la copia local completa cuando corresponda (PS §4.1). *Es una salida de la decisión, no una responsabilidad de decisión.*
- **D4.** ⟂ Si `local >= current` → nada; si `local < current` → incremental desde V+1 (MAP H1.1; derivado de IR §14.2 + PS §6.2).

### E — Frescura sin depender de Kafka
- **E1.** Detectar y recuperar pérdidas de notificaciones comparando versión local vs `current-version` (IR §16; PS §8).
- **E2.** ⟂ Ejecutar esa comparación periódicamente (el "cómo" es decisión propia; IR solo exige poder detectar el atraso; MAP H1.8).

### F — Robustez, errores y estado
- **F1.** Timeouts, reintentos limitados y observables; prohibidos los ciclos ilimitados (PS §8; IR §18.4).
- **F2.** Antes de repetir una acción con efectos, reconciliar por los identificadores/versión persistidos (IR §18.4).
- **F3.** Informar el estado y los errores de sincronización y traducir errores de integración a errores propios (PS §4.1, §5.15).
- **F4.** Recuperarse de fallos sin corromper el estado local; la copia local no se ve afectada si cátedra está caída (PS §5.15, §8).
- **F5.** Persistir el progreso necesario para continuar tras un reinicio (PS §8, §10).

### G — Diagnóstico auxiliar (opcional)
- **G1.** (Opcional) Usar `alumnos:{groupId}:*` para claves auxiliares propias; estructura y TTL a decisión propia; nunca fuente autoritativa (IR §14.6).

### H — Pruebas y evidencias
- **H1.** Pruebas automatizadas que cubran la sincronización como comportamiento fundamental (PS §10.1).
- **H2.** Evidencias: inicialización desde snapshot · actualización incremental · recuperación por snapshot tras discontinuidad · procesamiento idempotente de un mensaje duplicado (PS §11).

---

## 2. Las 6 responsabilidades

| # | Responsabilidad | Items | Qué garantiza | Naturaleza |
| --- | --- | --- | --- | --- |
| **R1** | **Recepción** | C1, C2, C3, C8 | Traducir la notificación externa a una orden interna, sin decidir ni escribir | Adaptador *in* (Kafka) → caso de uso |
| **R2** | **Planificación** | D1, D2, D4 (D3 = salida) | Decidir qué modo corresponde: nada / incremental / snapshot | Lógica **pura** de dominio |
| **R3** | **Consulta** | A1-A4, B1, C4, C5, C6 | Traer datos y versiones de la cátedra (REST + Redis) | Puertos *out* |
| **R4** | **Persistencia** | B2, B3, B4, B5, C7, C9 | Escribir la unidad de versión de forma consistente, atómica e idempotente | Escritura **única** compartida |
| **R5** | **Reconciliación** | E1, E2 | Disparar la puesta al día sin depender de Kafka | Disparador programado |
| **R6** | **Supervisión** | F1-F5 (+ G1 opcional) | Timeouts, reintentos, estado y observabilidad | Transversal |

### Mapeo SOLID

- **R1 — Recepción.** **SRP**: solo traduce; no decide ni escribe. **DIP**: el núcleo define el puerto de entrada y Kafka es un adaptador. **ISP**: el puerto pide solo el aviso (versión), nada más. **OCP**: ignorar campos desconocidos (C2) permite que el contrato evolucione sin tocar el núcleo.
- **R2 — Planificación.** **SRP**: decide y nada más; por eso es pura y testeable sin infraestructura (H1 sobre el hito 1.1). **OCP**: agregar un caso de decisión = agregar un test, sin tocar adaptadores. **DIP**: no conoce ni REST ni Redis.
- **R3 — Consulta.** **DIP**: define interfaces (puertos *out*) que la infraestructura REST/Redis implementa. **ISP**: puertos angostos (snapshot / versiones / cambios / entidades) en vez de un puerto único que lo sepa todo. **LSP**: los adaptadores (reales o dobles de capas propias) cumplen el mismo contrato.
- **R4 — Persistencia.** **SRP**: una única responsabilidad de escritura; snapshot, incremental y reconstrucción la reutilizan. **DIP**: el adaptador JPA vive en `catalog`, y `sync` escribe a través del puerto compartido para preservar la atomicidad de versión.
- **R5 — Reconciliación.** **SRP**: dispara, no decide ni escribe: reutiliza R2 + R3 + R4.
- **R6 — Supervisión.** Transversal: políticas separadas (timeouts, reintentos, reporte de estado). No debe mezclarse con la lógica de decisión ni de escritura.

### Puertos sugeridos (tentativos, no decisión)

Se listan solo como insumo; el modelado real de puertos/adaptadores es la sesión siguiente.

- **R1** → puerto *in* de notificación de nueva versión; adaptador `@KafkaListener` sobre `catedra.catalog.{groupId}`. Necesita un almacén de eventos procesados (dedup) → ver decisión abierta en §3.
- **R2** → sin puertos: función/objeto de dominio puro.
- **R3** → puertos *out*: snapshot REST; versiones/metadata; `changes:{v}`; entidades por ID desde los hashes.
- **R4** → puerto *out* de escritura del catálogo, implementado por el adaptador JPA ubicado en `catalog` (ARQUITECTURA §3).
- **R5** → puerto *in* de reconciliación disparado por un scheduler.
- **R6** → reporte de estado/observabilidad; opcionalmente el namespace `alumnos:{groupId}:*`.

### Modos resultantes (no son responsabilidades ni clases)

- **Snapshot**: R2 decide snapshot → R3 obtiene el snapshot → R4 lo aplica.
- **Incremental**: R1 avisa → R2 decide incremental desde V+1 → R3 lee `changes:{v}` + entidades → R4 aplica en orden y avanza versión.
- **Reconstrucción**: R2 detecta discontinuidad → snapshot (misma R3 + R4). R5 puede disparar cualquiera de los dos según la decisión de R2.

---

## 3. Decisiones abiertas (no resueltas acá)

Cada decisión se registra en `ARQUITECTURA.md` cuando se tome.

| Responsabilidad | Decisión abierta | Fuente |
| --- | --- | --- |
| R1 | Dónde se persiste el `eventId` para dedup (tabla propia vs `alumnos:{gid}:*`) | ARQ §4; MAP H1.7; IR §15.3, §18.1 |
| R3 | Timeouts y backoff hacia la cátedra | ARQ §4 |
| R3 | Estrategia de rotación/renovación del JWT técnico | IR §5.2, §5.4 |
| R3 | Uso (o no) del namespace `alumnos:{gid}:*` | IR §14.6; ARQ §4 |
| R4 | Estrategia transaccional de la sincronización (garantizar versión + datos juntos) | ARQ §4; PS §10 |
| R4 | Regla de baja física: qué hacer con IDs que no vienen en el snapshot | IR §7; MAP H1.4 |
| R4 | `day_of_week` enum vs `varchar(9)`, PK e índices; fila única de `catalog_version` | CU_2 §2.1; ARQ §4 |
| R5 | Criterio y frecuencia de la reconciliación periódica | MAP H1.8 |
| R6 | Timeouts, reintentos, límites y observabilidad; dónde se informa el estado de sync | ARQ §4; IR §18.4; PS §4.1 |
| Transversal | Herramienta de migraciones (Flyway/Liquibase) | ARQ §4; MAP H0.2 |

---

## 4. Límites: qué NO es del slice `sync`

- Búsqueda y filtros (categoría, nombre, `enabled`, disponibilidad) → contexto **`catalog`** (PS §4.1, §6.2; CU_2 §2.2).
- Contrato interno de agenda vigente hacia TurnosYReservas → contexto **`catalog`** (PS §4.2).
- Registro/login de usuarios finales y emisión del JWT de usuario → contexto **`catalog`** (ARQUITECTURA §3).
- Reservas, holds y Kafka de turnos → repo **TurnosYReservas**.
- **Sin stub de la cátedra**: no se crea ningún mock/emulador de REST, Redis ni Kafka; la verificación de contrato es manual contra la instancia real (IR §3; ARQUITECTURA §3).

---

## 5. Evidencias que este diseño debe poder probar (PS §11)

- Inicialización desde snapshot (H2).
- Actualización incremental del catálogo (H2).
- Recuperación mediante snapshot luego de una discontinuidad (H2).
- Procesamiento idempotente de un mensaje duplicado (H2).
- Pruebas automatizadas de la sincronización (H1; PS §10.1).
