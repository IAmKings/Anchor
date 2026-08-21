# Error Handling

> This app has no HTTP clients and no error-code JSON. Failures are Kotlin exceptions with Chinese user-facing messages, or nullable status strings on the host.

---

## Error types

There is no custom sealed `AppError` type.

| Mechanism | When |
|-----------|------|
| `require(...) { "中文。" }` | Caller passed illegal domain input (blank worry, unknown emotion word, score out of range). Throws `IllegalArgumentException`. |
| `check(...) { "中文。" }` | Internal invariant (Keystore blob, existing DB without a key). Throws `IllegalStateException`. |
| `error("未知安全动作：$this")` | Corrupt stored enum / mapping. |
| Nullable `String` status on `App(...)` | Host operations: export, delete, speech, lock (`exportStatus`, `deleteStatus`, `speechStatus`, `appLockMessage`). |

Do not add Result/Either wrappers around `AnchorStore` unless a call site genuinely has a recoverable branch beyond showing the exception message.

---

## Pattern

Validate in the store (or `SafetyPolicy`), not only in the composable.

```kotlin
internal fun validateEmotionCard(emotion: String, event: String, hardestPart: String) {
    require(emotion in emotionVocabulary) { "请选择一个具体情绪词。" }
    require(event.isNotBlank()) { "请写下发生了什么。" }
    require(hardestPart.isNotBlank()) { "请写下最难受的具体部分。" }
}
```

Messages are complete sentences in Chinese, usually ending with `。`. They are safe to show in UI. Do not throw English `require` text for user-editable fields (`AndroidBackgroundTimer` is host-internal and may use English).

UI: catch at the screen that invoked the store, assign `message` to a `var` / `status` field, and display it. Do not crash the composition root.

Host example: `MainActivity` maps missing mic permission to `"未获得麦克风权限。"` instead of throwing.

---

## Restore and delete (must not half-apply)

Encrypted import validates the whole JSON **before** opening a SQL transaction. On failure, existing rows stay. See `AndroidEncryptedProbeStore.restoreFromJson`.

Delete-all is explicit and confirmed in Settings. Failure keeps the user on Settings and sets `deleteStatus`; success returns to first-run assessment. Do not swallow the failure and pretend the profile is empty.

---

## What is not an error

- Camera-log evaluative language (`cameraFactNeedsHint`): weak hint, save still succeeds.
- Rhythm with only wake or only light: allowed.
- Empty insights (`"记录还不够"`): insufficient local data, not a failure.
- Notification permission denied: write the in-app banner; do not throw.

---

## Tests

Store and policy tests use:

```kotlin
assertFailsWith<IllegalArgumentException> { store.addEmotionCard("难过", "今天开会", "没人回应", 3_000) }
```

SQL mapping tests may use `runCatching` and assert the message contains a distinctive fragment (`"具体情绪词"`).

When adding a `require`, add a failing-input assertion next to the happy path in `InMemoryAnchorStoreTest` or the feature `*CopyTest` / policy test.

---

## Anti-patterns

- HTTP status bodies, `ProblemDetail`, or Retrofit `HttpException`.
- Logging the exception with the worry / emotion / PHQ answers attached.
- `catch (e: Exception) {}` around `evaluateAndStore`.
- English-only validation messages on fields the user just typed.
- Using `error()` for normal empty input (that is `require`).
