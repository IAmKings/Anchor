# Compose UI

> Shared Material 3 UI in `commonMain`. Copy is data; screens are layout.

---

## Composition root

`App.kt` owns:

- Light / dark `ColorScheme` (`AnchorColors` / `AnchorDarkColors`)
- Boolean `*Visible` flags for screens (no Navigation-Compose graph)
- Wiring from `AnchorStore` to feature screens
- Host callbacks (timers, export, lock, speech) as parameters with no-op defaults so `commonTest` and previews do not need Android

Add a screen by extending those flags and callbacks. Do not introduce a nav library for a fourth tab.

Theme tokens:

- Primary green `0xFF466552` (light) / `0xFFACCFB8` (dark)
- Secondary terracotta-equivalent `secondary` / `onSecondary` — use these for help / delete / crisis actions **and** the matching warning text (item 9 note, delete confirmations, step labels)
- Background parchment `0xFFFCF9F3` / warm black `0xFF1C1B18`

Do not reintroduce `Color(0xFFB26A4F)` or a local `Terracotta`. Crisis emphasis is `secondary`, not Material `error`.

---

## Copy extraction

User-visible Chinese lives in `*Copy.kt` as `internal const val` / `internal fun`, then the screen reads those names.

Example: `home/HomeCopy.kt` + `home/HomeCopyTest.kt`.

Rules:

- Test copy that branches (empty vs pending worry, running vs completed micro-action, insight empty state).
- Look up `CONTEXT.md` before writing a new phrase. Forbidden product language: streak, 完成率, 补卡, 鸡汤 encouragement, "打卡成功".
- Insights empty state is `"记录还不够"`, not "去完成更多任务".
- Micro-action CTA after finish is `"已记下"`, not "已完成打卡".
- One-thing lock: `"这 14 天只练「…」。不是任务。"` — no "必须".

Design PNG/HTML under `design/` is a visual reference. If it conflicts with PRD / CONTEXT / `CrisisResources`, the repo code and PRD win (this already happened for regional hotlines).

---

## Accessibility

M6 raised small captions toward **14sp**. User-visible text must not ship at 12sp or 13sp.

- Body / captions: ≥ 14sp
- Primary actions: ~17sp, 52–56.dp tall pills
- Respect system font scale; pages that grow (first-run welcome, settings) must scroll
- Semantics: `heading()`, `contentDescription` on icon-only controls (`"此刻需要帮助"`), `Role.Tab` on the bottom bar

TalkBack / 200% font regressions are Android-device checks, not JVM unit tests. Do not remove `verticalScroll` from first-run or settings to "match the mock".

---

## Medical waiting and previews

Practice home vs waiting home is selected from `store.safetyState().mode`, not from a remembered tab.

Settings includes preview controls for relation banners, waiting, and one-thing lock. Previews **must not** call `evaluateAndStore` or `enterCrisisWaiting()`. After closing settings, the real `SafetyState` is unchanged.

---

## Layout habits

- Card corner 16.dp, pill buttons ~28.dp / 999.dp
- Bottom tabs: 今天 / 记录 / 洞察 / 我的 (`HomeTab`). Settings opens from **我的**, not the top bar.
- Shared `HomeTopBar(onHelp)`: brand mark + 「锚点」 heading + circular 「助」 (`contentDescription = "此刻需要帮助"`). Do not put SOS/设置 back on the chrome.
- Records hub is a dual-entry card page (emotion box + camera log), not a nested tab replace of the bottom bar
- Wave entry is the 192.dp home orb (title + 「等待中」 inside, 「难受的时候点这里」 below); keep it one-tap reachable in Normal mode

---

## Tests

Copy tests are JVM `kotlin.test` and should stay side-effect free:

```kotlin
assertEquals("点这里把念头放进去", homeWorryBody(0, false, "今天 20:00"))
assertEquals("已记下", homeMicroActionActionLabel(hasTitle = true, running = false, completed = true))
```

Do not assert pixel layout in commonTest. Device checks stay on Debug installs (`make install-debug`).

---

## Anti-patterns

- Hardcoded English UI (product copy is Chinese).
- Streak badges, progress rings toward a quota, or red "you missed yesterday".
- New 12sp captions "to fit the mock".
- Putting PHQ item-9 crisis handling only in UI without `SafetyPolicy`.
- Using Stitch hope-line `24` as a crisis number.
