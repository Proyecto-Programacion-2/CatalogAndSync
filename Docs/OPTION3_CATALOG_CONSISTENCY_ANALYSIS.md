# Option 3: Catalog-Context Consistency Service - Analysis

Since `SyncCatalogUseCaseImpl` now calls `catalogConsistencyPort.isStateVerifiable(localVersion)`, the logic should live in the **catalog bounded context** (data owner) and be exposed to sync via a port. This matches hexagonal architecture: sync depends on catalog's port, not the other way around.

## What needs to be created/changed

### 1) Port in catalog domain
Create: `catalog/domain/ports/out/CatalogConsistencyPort.java`

```java
package com.programacion2.catalogandsync.catalog.domain.ports.out;

import java.util.Optional;

public interface CatalogConsistencyPort {
    boolean isStateVerifiable(Optional<Long> localVersion);
}
```

This is what sync already expects. Keep it simple and return boolean with fail-safe semantics.

### 2) Implementation in catalog application
Create: `catalog/application/service/CatalogConsistencyService.java` (or `CatalogConsistencyChecker`)

Responsibilities (follow `Docs/LOCAL_STATE_VERIFIABILITY.md` checks in order):
1. **Version row exists** - if `localVersion.isEmpty()` -> `false` (matches current use case behavior)
2. **Read catalog_version metadata** - need `last_sync_mode` and `updated_at` from `catalog_version`. Right now `CatalogVersionReaderPort` only has `readVersion()` returning `Optional<Long>`. The `CatalogVersion` model has all three fields.
3. **Collection presence/counts** - verify `professional_category`, `professional`, `weekly_schedule` exist and are consistent when a version exists. If any missing/empty in an inconsistent way -> `false`
4. **Window/corruption/fail-safe** - if `localVersion < oldestAvailableVersion` treat as not safely verifiable for incremental purposes (planner will force snapshot anyway, but better to be explicit). Catch all exceptions -> `false` and log.

### 3) Read/count access to the three collections
There's currently **no port** to count/read `professional_category`, `professional`, `weekly_schedule`. You have:
- `CatalogWritePort` (write-only)
- `CatalogVersionReaderPort` (only reads version number)

You'll need either:
- **New port**: `catalog/domain/ports/out/CatalogReadPort` with methods like `long countCategories()`, `long countProfessionals()`, `long countSchedules()` (or `boolean hasCompleteCatalog()`). Simple and enough for the checks.
- **Or extend existing ports** - but better to keep read concerns separate from write.

The service will need to inject these count/read ports + `CatalogVersionReaderPort`.

### 4) CatalogVersionReader needs to return full model
`CatalogVersion` exists as a record. Options:
- **Extend** `CatalogVersionReaderPort` to add `Optional<CatalogVersion> findCatalogVersion()` (keep `readVersion()` for backward compat or deprecate)
- **Or create** a separate `CatalogVersionMetadataPort`

Given `JdbcCatalogVersionReaderAdapter` currently returns `Optional.empty()`, you'll need to implement it properly to read from `catalog_version` table (single row per CU_2).

### 5) Wiring
- Annotate `CatalogConsistencyService` with `@Service`/`@Component` so Spring can inject it where needed
- The port `CatalogConsistencyPort` will be implemented by this service - in hexagonal, the application service implements the outbound/domain port or the port is defined in domain and implemented by application service. Either way is fine as long as sync depends on `catalog.domain.ports.out.CatalogConsistencyPort`.
- Add the implementation to the module so it's picked up by component scan

## Suggested file layout

```
catalog/
├── application/
│   └── service/
│       └── CatalogConsistencyService.java  (NEW) - implements CatalogConsistencyPort
├── domain/
│   └── ports/
│       └── out/
│           ├── CatalogConsistencyPort.java      (NEW)
│           ├── CatalogReadPort.java             (NEW) - counts/existence
│           └── CatalogVersionReaderPort.java    (MODIFY) - add findCatalogVersion() or similar
└── infrastructure/
    └── persistence/
        └── adapter/
            ├── JdbcCatalogVersionReaderAdapter.java (MODIFY) - read full CatalogVersion
            └── JdbcCatalogReadAdapter.java         (NEW) - implement CatalogReadPort
```

## Notes & gotchas

- **Version info**: `VersionLedgerPort` gives `localVersion()`, `currentVersion()`, `oldestAvailableVersion()`. The consistency service only needs `localVersion` (passed in) plus catalog-side metadata/counts. Don't mix responsibilities - ledger is for versions/orchestration, catalog is for data consistency.
- **Fail-safe everywhere**: wrap in try/catch, log with context, return `false` on any exception. Matches the doc.
- **Atomicity is key**: if `catalog_version` has a local_version but any of the 3 collections is empty/missing -> `false`. Per IR §7 all 3 + version must be written in same transaction, so committed version implies committed collections.
- **Window check**: the use case already gets `oldestAvailableVersion` from ledger. The service could also know it, but easier to have use case pass it or have service call ledger? Better keep service focused on catalog state consistency. The "out of window" case is mentioned in doc - can be handled by service if it has access to ledger, or by use case logic. Since you chose option 3 (catalog owns consistency), maybe pass `oldestAvailableVersion` to `isStateVerifiable` or have service depend on `VersionLedgerPort`. Need to be careful about cross-context dependencies.

**Recommendation:** Start with the minimal port signature `isStateVerifiable(Optional<Long> localVersion, long oldestAvailableVersion)` to cover the window check cleanly, or have the service inject `VersionLedgerPort` directly (catalog service can depend on sync port? That would be reverse dependency - bad). Better to pass needed values from use case.

Also check `Docs/LOCAL_STATE_VERIFIABILITY.md` again - it says "localVersion < oldestAvailableVersion means incrementals aren't available; the use case should treat this as not safely verifiable" - so either place is fine as long as it's enforced. Since service owns consistency rules, passing `oldestAvailableVersion` is clean.