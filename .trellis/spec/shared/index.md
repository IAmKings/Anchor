# Shared Module Guidelines

> Coding contracts for the Compose Multiplatform `shared/` module and its Android/iOS hosts.
> Trellis init labelled this repo as "backend". That is wrong: there is no server, no HTTP API, and no account system.

This is a **local-only** Kotlin Multiplatform app. Product logic, SQLDelight storage, and Compose UI live in `shared/`. `androidApp/` and `iosApp/` are thin hosts for platform APIs.

Authoritative product language: [`CONTEXT.md`](../../../CONTEXT.md). Privacy and metrics: [`docs/adr/0001-local-only-no-account.md`](../../../docs/adr/0001-local-only-no-account.md), [`docs/adr/0003-anti-kpi-metrics.md`](../../../docs/adr/0003-anti-kpi-metrics.md).

---

## Pre-Development Checklist

Read these before writing code. Skip only the files that cannot apply to the change.

- [ ] [`CONTEXT.md`](../../../CONTEXT.md) — domain terms. Do not invent milder or more KPI-like wording.
- [ ] [Directory structure](./directory-structure.md) — where a new feature, copy file, host adapter, or test goes.
- [ ] [Safety kernel](./safety.md) — **required** if the change touches assessment, crisis, waiting mode, hotlines, or which practice entries are visible.
- [ ] [Storage](./storage.md) — **required** if the change persists, exports, restores, or deletes user records.
- [ ] [Error handling](./error-handling.md) — **required** if the change validates input or reports failure to the user.
- [ ] [Privacy](./privacy.md) — **required** if the change logs, networks, notifies, exports, or records audio.
- [ ] [Compose UI](./compose-ui.md) — **required** if the change is a screen, copy string, color, or font size.
- [ ] [Quality](./quality-guidelines.md) — tests, forbidden patterns, review gates.

Always also read [`.trellis/spec/guides/index.md`](../guides/index.md).

---

## Quality Check

After writing code, verify:

- [ ] No new network permission, HTTP client, analytics SDK, or remote log sink on `debug` / `release`. The `internal` build type's GitHub version check is the only exception ([privacy](./privacy.md), ADR-0004).
- [ ] `SafetyPolicy` still owns scoring / crisis / hysteresis; UI does not reimplement it ([safety](./safety.md)).
- [ ] Writes that must stay atomic still go through `queries.transaction` / `transactionWithResult` ([storage](./storage.md)).
- [ ] User-facing strings live in `*Copy.kt` (or a named `internal const`) and avoid streak / 完成率 / 打卡 language ([compose-ui](./compose-ui.md)).
- [ ] Validation failures use `require` / `check` with Chinese messages; tests use `assertFailsWith<IllegalArgumentException>` ([error-handling](./error-handling.md)).
- [ ] `make verify` still passes. Shared unit tests: `./gradlew :shared:testDebugUnitTest`.

---

## Guidelines Index

| Guide | Use when |
|-------|----------|
| [Directory structure](./directory-structure.md) | Adding a feature package, source set, or host adapter |
| [Storage](./storage.md) | Schema, SQLDelight, SQLCipher, `AnchorStore`, export / restore |
| [Safety kernel](./safety.md) | PHQ-9 / GAD-7, crisis, medical waiting, hotlines, one-thing lock |
| [Error handling](./error-handling.md) | Validation, restore refusal, UI error strings |
| [Privacy](./privacy.md) | Logging, network, notifications, audio, telemetry |
| [Compose UI](./compose-ui.md) | Screens, copy, theme, accessibility |
| [Quality](./quality-guidelines.md) | Tests, forbidden patterns, review |

---

## Runtime layers (this repo)

```
Compose screens (*Screen.kt)  →  copy / policy functions  →  AnchorStore
                                                      ↓
                              SQLDelight (EncryptedProbe.sq) + SQLCipher
                                                      ↓
                         androidApp / androidMain / iosMain host adapters
```

There is no API or service layer. Do not introduce Retrofit, Ktor, Firebase, or a DI graph for a single implementation.
