# Quality Guidelines

> What must stay true for every change in this repo.

---

## Forbidden patterns

| Don't | Why | Instead |
|-------|-----|---------|
| Network, analytics, crash SDK, `INTERNET` permission | ADR-0001 / ADR-0003; `verify_privacy.py` fails | Local SQLCipher + user-triggered encrypted export |
| Re-score PHQ/GAD in a composable | Diverges from `SafetyPolicy` | `store.evaluateAndStore` / `SafetyPolicy.evaluate` |
| Streak, 完成率, 补卡, shame copy | Anti-KPI; CONTEXT.md | `"记录还不够"`, `"已记下"`, `"暂停很正常…"` |
| Fuzzy emotion words (`难过`, `焦虑`) as stored labels | Vocabulary is 32 concrete words | `emotionVocabulary` + `validateEmotionCard` |
| `Color(0xFFB26A4F)` / local `Terracotta` | Dark theme and contrast drift | `MaterialTheme.colorScheme.secondary` for crisis/delete |
| Shared timer IDs for wave and micro-action | One alarm overwrites the other | `BackgroundTimerKind` |
| Editing old `N.sqm` migrations | Breaks devices already upgraded | Add `N+1.sqm` |
| Logging worry / scale answers | Privacy | No logger |
| Treating `design/` as hotter than PRD for hotlines | Already caused wrong numbers | `CrisisResources.kt` + PRD §10.4 |
| Resetting `SafetyState` from Settings preview | Corrupts hysteresis | Preview flags only |

---

## Required patterns

1. **Copy + test** for branching user-visible strings (`*Copy.kt` / `*CopyTest.kt`).
2. **Same store contract** on memory and SQL; validate in `validate*` helpers.
3. **Atomic safety writes** via `evaluateAndStore` transaction.
4. **Domain words** from [`CONTEXT.md`](../../../CONTEXT.md): 亚临床, 就医等待期, 情绪标签化, 双栏日志, 忧虑保险箱, 挂卡, 微行动, 预测偏差, 浪潮等待, 表演耗竭, 利他微任务, 一次一件, 锁屏脱敏.
5. **New user-visible type ≥ 14sp**.
6. **Instrument tests** use isolated names: SQLCipher `m0-encrypted-probe.db` (never `anchor.db`); lock prefs `app-lock-instrumented`; timers `InstrumentedA/B`; export cache `exports-instrumented`. Do not clear `cacheDir/exports`.

---

## Testing requirements

Trusted styles (copy these):

| Kind | Example | Asserts |
|------|---------|---------|
| Policy table | `SafetyPolicyTest` | Every band edge, item 9, clarification, 7-day recover |
| Store contract | `InMemoryAnchorStoreTest` | Persist, reject, convert worry→action, recover, `clearAllData` |
| SQL reopen | `SqlDelightAnchorStoreTest` | Profile + log survive new `SqlDelightAnchorStore` on same driver |
| Copy | `HomeCopyTest`, `CrisisResourcesTest` | Exact Chinese strings / hotlines |
| Insights math | `LocalInsightsTest` | Cross-midnight wake math; empty → null / "记录还不够" |

Commands:

```bash
make verify
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:installDebug     # or make install-debug
```

`shared:testDebugUnitTest` is the default gate for policy/copy/store. Do not claim a UI flow is done from unit tests alone if it depends on AlarmManager, Keystore, or TalkBack — say what was not device-checked.

Release signing and iOS device QA are not on the current critical path (see `IMPLEMENTATION_PLAN.md`).

---

## Code review checklist

- [ ] `CONTEXT.md` terms used; no streak / 鸡汤.
- [ ] Crisis / waiting still owned by `SafetyPolicy` + `SafetyMode`.
- [ ] Schema change has `.sq` + new `.sqm` + export/restore mapping.
- [ ] `make verify` green (privacy scan included).
- [ ] Copy tests updated when strings change.
- [ ] Preview-only settings do not persist safety/relation records.
- [ ] Audio / export / notifications still local and lock-screen-safe.

---

## When a change is out of scope

Do not silently build: doctor PDF export, a fifth reminder class, lock-screen notification artwork, cloud sync, accounts, community, or an LLM chat. Those are listed as non-goals in the product plan and prior session notes. Create a Trellis task and wait for an explicit product decision.
