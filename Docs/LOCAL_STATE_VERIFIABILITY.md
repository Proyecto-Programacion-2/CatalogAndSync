# LOCAL_STATE_VERIFIABLE - How and When to Determine It

## Purpose

`localStateVerifiable` is a **safety flag** passed from `SyncCatalogUseCaseImpl` to `SyncPlanner.decide(...)`. 

- `SyncPlanner` is pure logic: it has no I/O, no Spring dependencies, and cannot inspect the database. It only decides based on the inputs it receives.
- Only `SyncCatalogUseCaseImpl` (the sync orchestrator) can compute `localStateVerifiable`. It is responsible for determining whether the local catalog state can be trusted.
- If `localStateVerifiable` is `false`, the planner **must return a `Snapshot` regardless of version numbers** (see `SyncPlannerTest.unverifiableLocalStateRequiresSnapshot()`).

> The key point: this flag is about **consistency of data**, not just "is there a version number?". A `local_version` can match `current-version` but the collections can be missing/corrupted from an interrupted write.

## Sources of Truth

To compute `localStateVerifiable`, the use case should use:

| Source | What it gives | Notes |
| --- | --- | --- |
| `VersionLedgerPort` | `localVersion()`, `currentVersion()`, `oldestAvailableVersion()` | Versions only. `localVersion()` comes via `CatalogVersionReader` (local DB), `currentVersion()`/`oldestAvailableVersion()` come from cátedra Redis. |
| `CatalogVersionReader` / `catalog_version` | Existence of the version row and metadata | `catalog/domain/model/CatalogVersion.java` has `localVersion`, `lastSyncMode`, `updatedAt`. The table is defined in `Docs/CU_2.md` (`catalog_version` with a single guaranteed row). |
| Catalog repositories/counts | Existence/counts of the three collections | `professional_category`, `professional`, `weekly_schedule`. Used to verify the catalog isn't partially written when a version exists. |
| `SyncException` / logging | Failure signals | Any I/O failure while verifying should be treated as "not verifiable" (fail-safe). |

## Checks (In Order)

Run these checks in order. **The first failing check must return `false`.** When in doubt, return `false` so the planner forces a snapshot.

| Order | Check | Expected for `true` | How to detect |
| --- | --- | --- | --- |
| 1 | **Version row exists** | A `catalog_version` row exists. | `versionLedgerPort.localVersion()` returns `Optional.empty()` → `false` (fresh/empty DB, must snapshot). |
| 2 | **Last sync not obviously failed** | No evidence the previous sync left an unknown/in-progress state. | Inspect `catalog_version.last_sync_mode` / `updated_at` (if available). If the last mode is suspicious, or updatedAt indicates a failed/aborted attempt with no commit, treat as `false`. If not yet modeled, infer from consistency (step 3). |
| 3 | **Atomicity of last apply** | The collections were written in the same transaction as advancing the version. | Per IR §7 and `Docs/MAP.md:42/53`, snapshot/incremental must apply all 3 collections + version in a single transaction. If a crash happened mid-apply, the transaction rolled back and the version was **not** advanced. Therefore, if the version exists but collections are incomplete/missing, the state is inconsistent → `false`. |
| 4 | **Collection presence/counts** | All three collections exist and are consistent with a valid catalog when a local version is present. | Inject catalog-side repositories/readers and count `professional_category`, `professional`, `weekly_schedule`. If any collection is missing/empty while a local version is present (and it's not a valid intentional state), → `false`. Counts must be checked, not just "table exists". |
| 5 | **Referential sanity (optional)** | Basic referential consistency between collections. | If cheap to check (e.g. professionals reference existing categories), do so. If expensive/schema-dependent or not yet trivial, **skip** for now. Do not block snapshot on a nice-to-have check. |

## Use Cases Where It Is False

| Use Case | Why | How to Detect |
| --- | --- | --- |
| **Fresh/empty database** | No local catalog state exists yet. | `localVersion()` returns `Optional.empty()` |
| **Crashed mid-apply (snapshot or incremental)** | Atomicity broken. Collections partially written; transaction rolled back so version may or may not reflect a committed state. | `catalog_version` exists but counts of one/more of the three collections are 0 or inconsistent with expectations → `false`. Prefer snapshot. |
| **Local version out of window** | Cannot safely apply incrementals (IR §18.2 / `Docs/MAP.md:39`). | `localVersion().get() < oldestAvailableVersion()`. Even if data exists, it's not verifiable to incrementally apply → treat as not verifiable for incremental purposes and let the planner force snapshot. |
| **Corrupted/inconsistent state** | Data doesn't match what a committed version implies. | Any of checks 2-4 fail (inconsistent counts, orphaned state that can't be verified, etc.). |
| **Unverifiable local state (forced)** | Explicit "unknown" condition (matches the test). | When the use case cannot prove consistency for any reason → `false`. This is exactly `SyncPlannerTest.unverifiableLocalStateRequiresSnapshot()`. |
| **Post-failure reconciliation uncertainty** | Previous sync/reconciliation failed mid-way or left state unknown. | Evidence from `last_sync_mode`/`updated_at` or failed verification checks → `false`. |

**Note:** A local version equal to or ahead of current with corrupt data must still be `false` (verifiability takes precedence over version equality). The planner will force a snapshot in that case.

## Implementation Pseudocode (for `SyncCatalogUseCaseImpl`)

```java
@Override
public SyncOutcome sync() {
    // 1) Read versions from ledger
    Optional<Long> localVersion = versionLedgerPort.localVersion();
    long currentVersion = versionLedgerPort.currentVersion();
    long oldestAvailableVersion = versionLedgerPort.oldestAvailableVersion();

    // 2) Determine local state verifiability
    boolean localStateVerifiable = isLocalStateVerifiable(localVersion);

    // 3) Decide
    SyncDecision decision = syncPlanner.decide(
        localVersion,
        currentVersion,
        oldestAvailableVersion,
        localStateVerifiable
    );

    // 4) Execute based on decision (snapshot/incremental/nothing)...
    return null; // to be implemented
}

private boolean isLocalStateVerifiable(Optional<Long> localVersion) {
    try {
        // Check 1: version row exists
        if (localVersion.isEmpty()) {
            return false;
        }

        // Check 2: metadata / last sync state (if available)
        // Optional: read CatalogVersion via CatalogVersionReader to inspect lastSyncMode/updatedAt
        // If suspicious -> return false

        // Check 3 & 4: atomicity + collection counts
        // Inject catalog repositories/readers and verify:
        // - professional_category count >= 0? but if version exists, collections must be consistent
        // - professional count
        // - weekly_schedule count
        // If any collection is missing/empty in a way inconsistent with a committed version -> false
        // If counts can't be verified due to I/O -> false (fail-safe)

        // Check 5: referential sanity (optional, skip if not cheap)

        return true;
    } catch (Exception e) {
        log.warn("Could not verify local state, forcing snapshot", e);
        return false; // fail-safe
    }
}
```

## Notes

- **Ownership:** `SyncCatalogUseCaseImpl` computes this. `SyncPlanner` never does I/O. `VersionLedgerPort` only reports versions.
- **Dependencies:** The use case will need catalog-side access (e.g. `CatalogVersionReader` from `catalog/domain/ports/out` and count methods on catalog repositories) to perform consistency checks. This is consistent with existing cross-context dependencies like `CatalogWritePort`.
- **Atomicity is king:** IR §7 requires applying the three collections + `snapshotVersion` in a single transaction. A committed version implies committed collections. Anything that cannot prove this is `false`.
- **Fail-safe:** When in doubt → `false` → planner returns `Snapshot`. Snapshot is safer than attempting an incremental from an inconsistent state.
- **Current state:** `SyncCatalogUseCaseImpl:37` hardcodes `localStateVerifible = true` (scaffolding). This document is the target behavior to replace it with. 
- **Window check:** `localVersion < oldestAvailableVersion` means incrementals aren't available; the use case should treat this as not safely verifiable for incremental application (the planner will also force snapshot, but being explicit is correct).
- **Local >= current:** Even if equal/ahead, verifiability still takes precedence. If inconsistent, return `false` to force snapshot.
