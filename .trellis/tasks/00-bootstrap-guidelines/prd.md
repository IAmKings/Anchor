# Bootstrap Task: Fill Project Development Guidelines

**You (the AI) are running this task. The developer does not read this file.**

Populate `.trellis/spec/` from this repo's real conventions. Trellis init scaffolded a **backend** layer; this project is a local Compose Multiplatform app with no server. Specs now live under `.trellis/spec/shared/`.

---

## Status

- [x] Replace the backend template with a `shared/` spec layer that matches the CMP layout
- [x] Fill guidelines with source-backed rules, file paths, and anti-patterns
- [x] Add code examples (store transaction, `SafetyPolicy`, copy tests, privacy gate)
- [x] Point thinking guides at Compose → Store → SQLDelight instead of API → Service

---

## Architecture context

- Single Gradle tree: `shared/` (domain + SQLDelight + Compose), `androidApp/` host, `iosApp/` shell.
- No account, no HTTP, no analytics (ADR-0001, ADR-0003). Gate: `scripts/verify_privacy.py`.
- Kernel: `SafetyPolicy` + `AnchorStore`. UI must not re-score scales.
- GitNexus is not indexed for this repo; specs were filled from source and tests.

---

## Spec files

| File | What it documents |
|------|-------------------|
| `.trellis/spec/shared/index.md` | Checklist + quality gate |
| `.trellis/spec/shared/directory-structure.md` | Modules, source sets, feature packages |
| `.trellis/spec/shared/storage.md` | SQLDelight, SQLCipher, store seam, export/restore |
| `.trellis/spec/shared/safety.md` | Scoring, crisis, hysteresis, hotlines, one-thing lock |
| `.trellis/spec/shared/error-handling.md` | `require`/`check`, Chinese messages, restore refusal |
| `.trellis/spec/shared/privacy.md` | No logger, no network, lock-screen, export |
| `.trellis/spec/shared/compose-ui.md` | Copy files, theme, 14sp, anti-KPI language |
| `.trellis/spec/shared/quality-guidelines.md` | Forbidden patterns, tests, review |

Removed: `.trellis/spec/backend/*` (empty templates that described a server this repo does not have).

---

## Acceptance criteria

- [x] Specs contain concrete examples and anti-patterns from the repository
- [x] No placeholder text remains (`To be filled`, `TODO: fill`)
- [x] Index files match the final spec files
- [x] Claims are backed by source files, tests, or project docs
- [x] Product Kotlin was not modified for this task
