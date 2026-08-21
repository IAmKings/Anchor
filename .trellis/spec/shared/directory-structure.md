# Directory Structure

> Where code lives in this Compose Multiplatform repo.

---

## Repository layout

```
shared/                 Kotlin Multiplatform: domain, storage, Compose UI, tests
androidApp/             Android host: Activity, biometrics, export share, speech
iosApp/                 SwiftUI shell + shared static framework (`Shared`)
design/                 Stitch mockups (visual reference, not source of truth for copy or hotlines)
docs/adr/               Architecture decisions
PRD.md                  Product requirements
CONTEXT.md              Domain vocabulary for implementers
scripts/                verify_prd.py, verify_privacy.py, install_debug.sh
```

Do not split `shared/` into extra Gradle modules unless build time or ownership forces it. New features are packages under `com.anchor.app`, not new modules.

Reference: [`README.md`](../../../README.md), [`IMPLEMENTATION_PLAN.md`](../../../IMPLEMENTATION_PLAN.md) §3.

---

## `shared/` source sets

| Source set | Owns |
|------------|------|
| `commonMain` | Domain types, `SafetyPolicy`, `AnchorStore`, Compose screens, copy |
| `androidMain` | SQLCipher driver, Keystore-wrapped DB key, AlarmManager timers, reminders |
| `iosMain` | Native SQLDelight driver wrapper |
| `commonTest` | `kotlin.test` for policy, copy, in-memory store, insights |
| `androidUnitTest` | SQLDelight on `JdbcSqliteDriver` (no SQLCipher) |
| `androidInstrumentedTest` | Device SQLCipher / migration tests |

Hosts:

- `androidApp/src/main/kotlin/com/anchor/app/MainActivity.kt` constructs `AndroidEncryptedProbeStore`, timers, lock, export, and calls `App(...)`.
- `iosApp/` is a launch shell. iOS device verification is deferred; do not block Android work on it.

---

## Feature packages

All product Kotlin is under `shared/src/commonMain/kotlin/com/anchor/app/<feature>/`.

| Package | Feature |
|---------|---------|
| `safety/` | `SafetyPolicy`, crisis copy, hotlines, help / guide / waiting screens |
| `storage/` | `AnchorStore`, SQLDelight impl, export mapping, validations |
| `onboarding/` | First-run assessment, first-anchor choice, reassessment |
| `home/` | Today screen + home copy |
| `emotion/` | Emotion cards |
| `journal/` | Camera log + records hub |
| `worry/` | Vault + hang sheet |
| `action/` | Micro-action run + history evidence |
| `rhythm/` | Wake / light |
| `relation/` | Performance drain + altruism |
| `insights/` | Local-only stats (no streak) |
| `wave/` | Wave waiting |
| `settings/` | Lock, reminders, export, delete |
| `ui/` | Tiny shared formatters (`formatCountdown`) |

`App.kt` is the composition root: it holds boolean visibility flags and wires screens to `AnchorStore`. There is **no** Navigation Compose graph and **no** `expect`/`actual` API layer. Platform types are ordinary classes in `androidMain` / `iosMain` / `androidApp`.

When adding a feature:

1. Put domain types and store methods in `storage/AnchorStore.kt` (interface + `InMemoryAnchorStore`).
2. Implement the same methods on `SqlDelightAnchorStore`.
3. Add SQL in `EncryptedProbe.sq` plus a numbered migration under `shared/src/commonMain/sqldelight/migrations/`.
4. Put UI in `<feature>/<Name>Screen.kt` and strings in `<feature>/<Name>Copy.kt`.
5. Put tests next to the feature under `commonTest/.../<feature>/`.

---

## Naming

- Files: `PascalCase.kt` for types/screens, `*Copy.kt` for user-visible strings, `*Test.kt` for tests.
- Packages: lowercase feature names (`worry`, not `worryVault`).
- Store models: `data class` + `enum class` in `AnchorStore.kt`.
- SQL tables / columns: `snake_case` (`first_low_assessment_at`). Kotlin uses `camelCase` + `Millis` suffix for timestamps (`completedAtMillis`).
- SQLDelight database name: `AnchorDatabase`, queries type `EncryptedProbeQueries` (historical name from the M0 probe; do not rename without a migration plan).

---

## Host vs shared

Keep Compose and domain in `commonMain`. Put only what needs Android/iOS APIs in the host or `androidMain`:

- Database open + SQLCipher key: `AndroidDatabaseKey`, `AndroidEncryptedProbeStore`
- Background timers: `AndroidBackgroundTimer` (separate request codes for wave vs micro-action)
- Reminders: `AndroidReminderScheduler`
- Biometrics, speech, FileProvider share: `androidApp`

Do not move `SafetyPolicy` or copy tests into Android-only source sets.

---

## Examples to copy

- Feature slice with copy + tests: `shared/src/commonMain/kotlin/com/anchor/app/home/HomeCopy.kt` + `HomeCopyTest.kt`
- Store contract test: `shared/src/commonTest/kotlin/com/anchor/app/storage/InMemoryAnchorStoreTest.kt`
- SQLDelight reopen test: `shared/src/androidUnitTest/kotlin/com/anchor/app/storage/SqlDelightAnchorStoreTest.kt`
- Host wiring: `androidApp/src/main/kotlin/com/anchor/app/MainActivity.kt`

---

## Anti-patterns

- New Gradle module for one screen.
- `expect`/`actual` wrappers around a single Android class (current code uses concrete `androidMain` types).
- Navigation library, Hilt/Koin, or a Retrofit "API" package.
- Putting user-visible Chinese strings only inside a composable when the same phrase is tested or reused — extract to `*Copy.kt`.
- Treating `design/` mock copy or placeholder hotlines as source of truth. Hotlines follow PRD §10.4 via `CrisisResources.kt`.
