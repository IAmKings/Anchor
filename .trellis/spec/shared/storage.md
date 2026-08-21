# Storage

> Local encrypted persistence. There is no remote database.

---

## Stack

- SQLDelight **2.3.2**, database `AnchorDatabase`, package `com.anchor.app.db`.
- Android production: SQLCipher **4.17.0** via `SupportOpenHelperFactory`.
- Random 256-bit passphrase, wrapped by a non-exportable Android Keystore AES-GCM key (`AndroidDatabaseKey`).
- Current schema: `shared/src/commonMain/sqldelight/com/anchor/app/db/EncryptedProbe.sq`.
- Upgrades: numbered files `shared/src/commonMain/sqldelight/migrations/1.sqm` … `13.sqm`. Latest checked-in migration adds `user_profile.first_anchor_at`.

`encrypted_probe` is a leftover M0 table (single-row probe). Leave it; new data goes in the feature tables beside it.

---

## Store seam

`AnchorStore` is the only persistence API the UI may call.

| Implementation | Role |
|----------------|------|
| `InMemoryAnchorStore` | Default in `App()` when no store is passed; `commonTest` contract |
| `SqlDelightAnchorStore` (`internal`) | Shared SQL mapping + validation |
| `AndroidEncryptedProbeStore` | SQLCipher driver + restore + `clearAllData` file cleanup; delegates to `SqlDelightAnchorStore` |
| `IosEncryptedProbeStore` | Native driver wrapper |

`App(anchorStore = null)` uses memory. Production Android always injects `AndroidEncryptedProbeStore`.

Both memory and SQL paths must keep the same validation and state-evolution contract. Prove new store methods in `InMemoryAnchorStoreTest` (common) and, for SQL mapping / reopen, `SqlDelightAnchorStoreTest` (androidUnitTest).

---

## Signatures (core)

```kotlin
interface AnchorStore {
    fun evaluateAndStore(input: AssessmentInput): SafetyOutcome
    fun enterCrisisWaiting()
    fun safetyState(): SafetyState
    fun saveUserProfile(profile: UserProfile)
    fun clearAllData()
    // plus emotion / camera log / worry / micro-action / rhythm / relation writes
}
```

`evaluateAndStore` **must** call `SafetyPolicy.evaluate` with the previous `safetyState()`, then persist the assessment row **and** `safety_state` together.

SQL implementation:

```kotlin
override fun evaluateAndStore(input: AssessmentInput): SafetyOutcome = queries.transactionWithResult {
    val outcome = SafetyPolicy.evaluate(/* ... previous = safetyState() ... */)
    queries.insertAssessment(/* ... */)
    queries.upsertSafetyState(outcome.state.mode.name, outcome.state.firstLowAssessmentAtMillis)
    outcome
}
```

`resolveWorryAsAction` is the other required transaction: insert `micro_action` with `source_worry_id` and mark the worry resolved, or neither happens.

---

## Schema contracts

- Timestamps are `INTEGER` epoch millis.
- Booleans are `INTEGER` 0/1 with `CHECK (... IN (0, 1))`.
- Singleton rows (`safety_state`, `user_profile`, `encrypted_probe`) use `id = 1`.
- Enums persist as Kotlin `enum.name` (`SafetyMode.MedicalWaiting` → `"MedicalWaiting"`). Map through `StorageMapping.kt` (`storageName()`, `toSafetyAction()`), not ad-hoc `when` in screens.
- PHQ/GAD answer lists persist as comma-separated digits (`"0,1,2,..."`), parsed by `String.toAnswers()`.
- `emotion` must be a member of `emotionVocabulary` (32 concrete words). Fuzzy words like `"难过"` are rejected.
- `rhythm_entry` requires wake **or** light (`CHECK (wake_at IS NOT NULL OR light_at IS NOT NULL)`).
- Micro-action difficulties are 1–10; `source_worry_id` is `UNIQUE` so one worry converts at most once.

Validation lives as `internal fun validate*` in `AnchorStore.kt` and is reused by SQL, memory, and JSON restore. Do not duplicate checks in the composable.

---

## Encryption and keys

`AndroidDatabaseKey.getOrCreate(context, databaseName)`:

- If a wrapped key exists in private SharedPreferences, unwrap it.
- If the database file already exists and the key is missing, **fail** (`check`) — do not create a new key that would brick or silently recreate the file.
- New passphrase: 32 random bytes, AES-GCM wrapped, version byte + IV + ciphertext, Base64 in prefs.

SQLCipher key format for the driver is `ByteArray.toSqlCipherKey()` (`x'<hex>'`).

---

## Export / restore

`buildLocalExport(store)` emits JSON `exportVersion: 3` and a three-column CSV. JSON includes `hasAudio` for worry cards but **not** audio bytes or file paths.

Restore (`AndroidEncryptedProbeStore.restoreFromJson`):

1. Validate version (`2..3`), enums, answer lengths, and `validate*` on every record.
2. Only then `queries.transaction { clear all tables; insert }`.
3. Bad password / bad file / unsupported version must leave existing data untouched.

`clearAllData()` deletes SQL rows, local audio, export cache, timers, and reminders, then returns the user to first-run assessment.

---

## Validation & error matrix

| Condition | Effect |
|-----------|--------|
| Emotion not in `emotionVocabulary` | `IllegalArgumentException` `"请选择一个具体情绪词。"` |
| Both camera-log columns blank | `"至少写下一栏。"` |
| Worry has neither text nor audio | `"请写下或录下要挂起来的念头。"` |
| Rhythm with neither wake nor light | `"至少记录起床或见光时间。"` |
| Predicted/actual difficulty outside 1..10 | `"预测困难度必须在 1 到 10 之间。"` / `"实际体感必须在 1 到 10 之间。"` |
| Restore `exportVersion` not in 2..3 | `"不支持的备份版本。"` — no writes |
| Missing Keystore-wrapped key but DB file exists | `check` failure, do not mint a new key |

---

## Good / base / bad

- **Good**: `evaluateAndStore` high PHQ → `SafetyAction.MedicalWaiting` persisted; two low assessments ≥7 days apart recover to `SafetyState()`. Covered by `InMemoryAnchorStoreTest.followsTheSamePersistRecoverAndClearContract`.
- **Base**: `addCameraLog` with only a fact column succeeds; evaluative fact sets `factNeedsHint` without blocking.
- **Bad**: `addEmotionCard("难过", ...)` throws; SQL reopen test asserts the message contains `"具体情绪词"`.

---

## Tests required

- New store field: update `InMemoryAnchorStore`, `SqlDelightAnchorStore`, `.sq` + `N.sqm`, `buildLocalExport`, restore parser, and the in-memory contract test.
- Schema bump: add `N.sqm` (do not edit old migrations). Instrumented migration tests live in `AndroidEncryptedProbeStoreTest` — they mutate device state; do not run them against a user's daily profile.
- Export: `LocalExportTest` for JSON escaping and `hasAudio` without paths.

---

## Wrong vs correct

#### Wrong
Call `SafetyPolicy.evaluate` in a screen and then `upsertSafetyState` in a separate non-transactional write. Reimplement scoring in SQL.

#### Correct
`store.evaluateAndStore(AssessmentInput(...))` only. UI reads `SafetyOutcome` / `store.safetyState()`.

#### Wrong
Add a cloud Firestore or HTTP sync "just for backup".

#### Correct
User-initiated encrypted `.anchor` file via `buildLocalExport` + `AndroidEncryptedExport`. ADR-0001.

---

## Common mistakes

- Forgetting the numbered `.sqm` after editing `EncryptedProbe.sq` (fresh installs get `.sq`; existing devices need the migration).
- Persisting `SafetyAction` sealed types with `toString()` instead of `storageName()`.
- Exporting raw `audioFileName` paths (they are device-private and unrestorable).
- Clearing only some tables in restore; relation energy / altruism must be cleared with the rest.
- Using `InMemoryAnchorStore` in Android production (`MainActivity` must pass `AndroidEncryptedProbeStore`).
