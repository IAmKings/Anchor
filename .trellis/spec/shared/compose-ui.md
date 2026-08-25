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
- Primary actions: ~17sp, 52–56.dp tall pills. Wave waiting uses `heightIn(min = 52.dp)` on 我看见了 / 继续 / 再加 10 分钟 / 回到今天.
- Wave locate chips show `shortLabel` and set `contentDescription` to `fullLabel`. Keep all eight PRD sites plus custom; do not shrink to the Stitch four-cell mock.
- Respect system font scale; pages that grow (first-run welcome, settings) must scroll
- First-run welcome: age chips `heightIn(min = 52.dp)`; legal links are inline (用户协议 / 隐私政策). Keep 18+ / 14–17 chips and crisis-region picker even though the Stitch welcome mock omits them. Keep `ageDisclaimer` self-report wording; do not use the mock's 「专业、理性的辅助支持」.
- First-anchor picker: radio cards with a Chinese glyph well (not ✓ prefixes, not Material icons). Sticky bottom bar holds the 14-day hint and 「确认选择」. Option list is `p0FirstAnchors + p1FirstAnchors`, never the Stitch habit mock (正念呼吸 / 数字睡前).
- Semantics: `heading()`, `contentDescription` on icon-only controls (`"此刻需要帮助"`), `Role.Tab` on the bottom bar

TalkBack / 200% font regressions are Android-device checks, not JVM unit tests. Do not remove `verticalScroll` from first-run or settings to "match the mock".

---

## Medical waiting and previews

Practice home vs waiting home is selected from `store.safetyState().mode`, not from a remembered tab.

Waiting-home chrome: badge is **就医等待期** (not 医疗等待期). Body must include PRD §10.2 「暂停练习不是惩罚…过度自我要求」. Camera-log tool copy is facts, never 「感受」. Stitch waiting-mode HTML is layout reference only.

Help-now / crisis page: numbered glyph wells and a sticky emergency bar. Numbers come from `crisisResource`, never Stitch 「希望24」 or hardcoded 120/110. Do not add maps, contacts, or pep-talk (「好起来」). Primary/emergency buttons use `heightIn(min = 52.dp)`. First-run `CrisisResult` reuses `HelpNowCopy` and `HelpNowStepCard`. First-run `MedicalResult` reuses `homeWaitingBody`, waiting-home tool copy, HelpNow steps, and a sticky 「进入就医等待期」. Mild result uses a sticky 「选择第一个锚点」 and must not embed a micro-action radio list from the Stitch mild-result mock. `ScaleForm` (PHQ/GAD and reassessment) uses two-column answer chips with `heightIn(min = 52.dp)` and a sticky 「提交评估」; do not switch to four columns. Hang composer speech and seal use `heightIn(min = 52.dp)`. Worry vault `OverviewStep` 「现在就想处理 / 确认开箱 / 开始处理第一项」 use `heightIn(min = 52.dp)`; keep the locked rumination + count card (no card bodies) and the FR-3.3 confirm path. Do not put Stitch hover 「不再重要 / 暂时无解」 on overview preview cards — those belong to `ProcessStep`. `ProcessStep` three choices and play-audio use `heightIn(min = 52.dp)`. 「暂时无解」 subtitle is `vaultUnsolvableHint(nextSessionLabel)` (`[下次专场] 前无需再想`), never a hardcoded 今晚/明晚 20:00. Three-choice stays encouraged, not forced. Do not add Stitch strike-through / archive motion. `ConvertStep` 「确认并同步到首页」 uses `heightIn(min = 52.dp)` and stays disabled when the action is blank; reuse `vaultQuotedCard`. Do not change `resolveWorryAsAction`. `DoneStep` 「回到今天」 uses `heightIn(min = 52.dp)`; keep `vaultDoneSummary` as an accepted-count, not a rate. Do not add Stitch 「查看计划」。Micro-action Pick/Predict/Run/Rate/Result primary buttons, 1–10 score chips, and pick cards use `heightIn(min = 52.dp)`. Keep the product preset list (「做 5 个深蹲」), not the Stitch recommendation mock or 「做 5 个深呼吸」. Timer finish stays 「我已完成」; home after recording stays 「已记下」. Do not change `MICRO_ACTION_MILLIS`. Micro-action history evidence rows use `heightIn(min = 52.dp)`. Keep `biasCopy` (points, not %) and the source quote 「那张表」. Do not use Stitch 60% / 「这张表」 / a nested 「查看历史记录」 on this page, and do not move `LocalInsights` charts here.

Settings includes preview controls for relation banners, waiting, and one-thing lock. Previews **must not** call `evaluateAndStore` or `enterCrisisWaiting()`. After closing settings, the real `SafetyState` is unchanged.

---

## Layout habits

- Card corner 16.dp, pill buttons ~28.dp / 999.dp
- Bottom tabs: 今天 / 记录 / 洞察 / 我的 (`HomeTab`). Settings opens from **我的**, not the top bar.
- Shared `HomeTopBar(onHelp)`: brand mark + 「锚点」 heading + circular 「助」 (`contentDescription = "此刻需要帮助"`). Do not put SOS/设置 back on the chrome.
- Records hub is a dual-entry card page (emotion box + camera log), not a nested tab replace of the bottom bar
- Wave entry is the 192.dp home orb (title + 「等待中」 inside, 「难受的时候点这里」 below); keep it one-tap reachable in Normal mode
- Practice-home daily flow (Stitch first-ship Chinese home): full-width 晨间节律 card, then micro-action | worry-vault side by side (`weight(1f)`, `IntrinsicSize.Min`). One-thing lock showing a single card → that card is full width. Relation/altruism stays a full-width `StatusCard` **below** the bento. No rhythm progress bar, 「温和开始」, or checkmark streak.

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
