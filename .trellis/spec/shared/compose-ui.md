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
- Icon wells use solid M3 tonal pairing: `container` background + `onContainer` icon tint (e.g. `tertiaryContainer` + `onTertiaryContainer`). Do not mix families (container from one role + icon from another) and do not apply alpha to the container — both read as mismatched on light theme (fixed 2026-10 sweep across home cards, records hub, journal, return-to-practice).
- Background parchment `0xFFFCF9F3` / warm black `0xFF1C1B18`

Do not reintroduce `Color(0xFFB26A4F)` or a local `Terracotta`. Crisis emphasis is `secondary`, not Material `error`.

Settings 「外观」 is three choices: 跟随系统 / 浅色 / 深色. Default is `ThemeChoice.System`, resolved by `ThemeChoice.useDark`. The choice is a device preference (Android `anchor-theme`, iOS `anchor.theme`), not an `AnchorStore` row, and delete-all does not clear it. Do not add more themes or a separate appearance page. The Android 12+ splash follows that choice, not only the phone's night mode: 浅色 is parchment `#FCF9F3`, 深色 is the night background, 跟随系统 follows the phone. Persist it with `UiModeManager.setApplicationNightMode` (`MODE_NIGHT_NO` / `YES`; `MODE_NIGHT_AUTO` clears an earlier override). Do not make the splash always black or always light. The first cold start after a choice change can still show the previous splash; the next one matches.

---

## Copy extraction

User-visible Chinese lives in `*Copy.kt` as `internal const val` / `internal fun`, then the screen reads those names.

Example: `home/HomeCopy.kt` + `home/HomeCopyTest.kt`.

Rules:

- Test copy that branches (empty vs pending worry, running vs completed micro-action, insight empty state).
- Look up `CONTEXT.md` before writing a new phrase. Forbidden product language: streak, 完成率, 补卡, 鸡汤 encouragement, "打卡成功".
- Insights empty state is `"记录还不够"`, not "去完成更多任务". Insights 「查看历史记录」 uses `heightIn(min = 52.dp)` and reuses `historyOpenLabel`. Three-month card value is 「看长期，不看某一天」, never Stitch 「15% 提升」. Relation energy / altruism insight cards keep `energyCopy` / `altruismFeelCopy` (counts). Extra is count bars + last-12 sparkline, not Stitch 表演耗竭率 / 75% / HRV. Pause uses `pauseAltruismCopy`. Monitor stays a count of 段, not a rate.
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
- First-run welcome: age chips `heightIn(min = 52.dp)`; legal consent is one inline sentence (`agreementPrefix` plus the 用户协议 / 隐私政策 links), not separate buttons that wrap the links onto their own line. The two links are spaced, underlined, and `secondary` — `primary` is too close to the sentence color to read as tappable. Tapping 用户协议 / 隐私政策 opens the in-app `LegalDocumentScreen` (offline full text from `LegalDoc.kt`, `AnchorBackBar` + section headings) — no WebView and no `INTERNET`. The website copy remains authoritative; the reader footer offers a user-initiated browser jump (`termsUrl`/`privacyUrl`) for the live version — do not replace the offline snapshot with a WebView. The mark above 「锚点 Anchor」 is `AnchorAppIcon` (the launcher anchor), not the character 「锚」. Mild and medical results, and both PHQ-9 / GAD-7 `ScaleForm`s, use that same mark above 「锚点 Anchor」. A page exit is `AnchorBackRow`: one row flush under the status bar, at least 48.dp, back icon and label on the start side. Never end-aligned, and never inside that page's own vertical padding. Scaffold screens put the row in `topBar`. Other screens use `AnchorBackBar` so the row stays outside the scroll. The row owns the system back gesture (edge swipe and the back key). The same row is the settings-preview welcome exit (labeled 返回, not a top-right 关闭), the reassessment invite, the wave exit (labeled 离开), and a full-screen editor's 取消. Nested flows use a contextual back row: the top-left back label names its destination (e.g. vault processing shows 「开箱」, conversion shows 「三选一」) and the system back gesture follows the same hierarchy; do not add in-body 「回到开箱」「回到三选一」 buttons (removed 2026-10 — they read as duplicates of the top-left back). 「换一个」 stays in the body and does not own the page gesture. Gestures attached to `pointerInput(Unit)` capture callbacks once — route close/discard handlers through `rememberUpdatedState` (see HangSheet `currentClose`; VoiceMemoButton does the same), or a mid-recording drag reads a stale `speechRecording=false`, skips discard, and orphans both the microphone and an unsealed ghost card. First-run welcome has no back row. Keep 18+ / 14–17 chips and crisis-region picker even though the Stitch welcome mock omits them. Keep `ageDisclaimer` self-report wording; do not use the mock's 「专业、理性的辅助支持」.
- Short page names sit in `AnchorBackRow` / `AnchorBackBar` `title`: 17.sp SemiBold, one line, ellipsis, centered on the full row. Both sides reserve the measured back-control width, so the title is not centered in the leftover space and does not cover the control. `heightIn(min = 48.dp)` lets large fonts grow. Do not put a sentence in the bar. This replaces the old in-body page name on: 我的, 洞察, 晨间节律, 微行动 (pick step only; 「动作完成」 stays in the body), 历史证据, 双栏日志 (list and new log; the new log's control stays 取消), 就医指南 (`homeWaitingGuideTitle`), 就医准备清单 (`homeWaitingChecklistTitle`), 关系与利他, 关系耗竭自测, 回血 / 抽干, 微小利他, and reassessment `ScaleForm` titles 「情绪自评 (PHQ-9)」 / 「焦虑自评 (GAD-7)」. First-run scales have no back row, so their title stays in the body. Emotion list uses `recordsEmotionTitle` 「情绪标签箱」 with `titleIsHeading = false` and keeps 「给情绪起一个准确的名字」 as the body heading; the new-emotion editor keeps 「哪一个词更接近？」 and gets no bar title. Reassessment invite uses the kicker (「复评邀请」, or 「就医等待期」 while waiting) with `titleIsHeading = false`; the headline stays the body heading. Worry vault adds `vaultTitle` 「忧虑保险箱」; 「转化动作」 and 「整理完毕」 stay in the body. Do not add a bar title to crisis 「你现在不是一个人」, wave (离开), settings-preview welcome, first-anchor, export 「文件已加密生成」, return-to-practice, outcome moments, or the records hub.
- Icon wells use an outlined Material `ImageVector` (`AnchorIconWell` or `Icon`), never a single character such as 助 / 医 / 光 / 锁 / 钥. Reuse `firstAnchorIcon` shapes where the concept matches; step numbers stay digits. The home start mark is `AnchorAppIcon` at 40.dp (the launcher tile, not the character 「锚」, not an `AnchorIconWell`). It has no content description. 「锚点」 stays the heading.
- First-anchor picker: radio cards with a language-independent icon well (`firstAnchorIcon`, not a Chinese character, not ✓ prefixes). Sticky bottom bar holds the 14-day hint and 「确认选择」. Option list is `choosableFirstAnchors` (给情绪起名 / 只记事实 / 固定节律 / 无损消耗 / 利他微任务), not the three resident tools and never the Stitch habit mock (正念呼吸 / 数字睡前). 「再加一件」 reuses that picker with `addAnchorHint` and 「加上这一件」, and does not restart the 14 days.
- Semantics: `heading()`, `contentDescription` on icon-only controls (`"此刻需要帮助"`), `Role.Tab` on the bottom bar

TalkBack / 200% font regressions are Android-device checks, not JVM unit tests. Do not remove `verticalScroll` from first-run or settings to "match the mock".

---

## Medical waiting and previews

Practice home vs waiting home is selected from `store.safetyState().mode`, not from a remembered tab.

Home standing notice is PRD FR-7.5 「本应用仅适用于轻度/亚临床调节。诊断与治疗请务必寻求专业医生帮助。」 not the shorter Stitch line. Worry vault open overview shows a desk-not-bed/sofa hint; no 「必须」.

Waiting-home chrome: badge is **就医等待期** (not 医疗等待期). The 「寻求专业支持」 well stays `secondaryContainer`, and its icon tint is `onSecondaryContainer`, not `primary`. The guide button stays the green primary action. Body must include PRD §10.2 「暂停练习不是惩罚…过度自我要求」. While already in waiting, the reassessment invite and the home 「再次评估」 row use `waitingReassessNotice`: first low, still inside the 7 days, or the second low can reopen practice. Do not reuse the 14-day 「14 天到了」 headline for that state, and do not call a low score 中度至重度 on the invite. A reassessment result that is still waiting uses `stillWaitingResult`: PHQ-9 and GAD-7 lines use `phqBandLabel`, the button is 「回到首页」, and a 0 is not 「中度至重度」. First entry into waiting keeps the entry pill and 「进入就医等待期」. Do not re-decide the 7-day exit in the composable; read `SafetyPolicy.stillWaitingKind`. Outside waiting, the invite stays the 14-day review. Camera-log tool copy is facts, never 「感受」. Stitch waiting-mode HTML is layout reference only.

Help-now / crisis page: numbered glyph wells and a sticky emergency bar. Numbers come from `crisisResource`, never Stitch 「希望24」 or hardcoded 120/110. Do not add maps, contacts, or pep-talk (「好起来」). Primary/emergency buttons use `heightIn(min = 52.dp)`. Each dialable number is its own control (`EmergencyDialButtons`, or a text button at least 48.dp). Two or more emergency numbers share one row, and the list spacing stays tight enough that the next card is cut through its body on the first screen; a single number stays full width. Do not fade that cut into the page background. The click calls `onDial` and stays on that page — do not close it, and do not point a dial control at `onClose` or `onFinish`. Scroll on a safety place is kept by `SaveableStateHolder` while that place stays on `safetyStack`, and dropped when it is popped. If the line is not entirely a phone number, keep the prose and add buttons only for the dialable pieces. Guide, checklist, and the guide's 「打开记录」 push `safetyStack`; back pops one level. Do not clear the parent flag to reveal a child. First-run `CrisisResult` reuses `HelpNowCopy` and `HelpNowStepCard`. The crisis bottom bar dials with `EmergencyDialButtons` and keeps 「先进入就医等待期」 as the exit. First-run `MedicalResult` reuses `homeWaitingBody`, waiting-home tool copy, HelpNow steps, and a sticky 「进入就医等待期」. Mild result uses a sticky 「选择第一个锚点」 and must not embed a micro-action radio list from the Stitch mild-result mock. `ScaleForm` (PHQ/GAD and reassessment) uses two-column answer chips with `heightIn(min = 52.dp)` and a sticky 「提交评估」; do not switch to four columns. Hang composer speech and seal use `heightIn(min = 52.dp)`. Worry vault `OverviewStep` 「现在就想处理 / 确认开箱 / 开始处理第一项」 use `heightIn(min = 52.dp)`; keep the locked rumination + count card (no card bodies) and the FR-3.3 confirm path. Do not put Stitch hover 「不再重要 / 暂时无解」 on overview preview cards — those belong to `ProcessStep`. `ProcessStep` three choices and play-audio use `heightIn(min = 52.dp)`. 「暂时无解」 subtitle is `vaultUnsolvableHint(nextSessionLabel)` (`[下次专场] 前无需再想`), never a hardcoded 今晚/明晚 20:00. Three-choice stays encouraged, not forced. Do not add Stitch strike-through / archive motion. `ConvertStep` 「确认并同步到首页」 uses `heightIn(min = 52.dp)` and stays disabled when the action is blank; reuse `vaultQuotedCard`. Do not change `resolveWorryAsAction`. `DoneStep` 「回到今天」 uses `heightIn(min = 52.dp)`; keep `vaultDoneSummary` as an accepted-count, not a rate. Do not add Stitch 「查看计划」。Micro-action Pick/Predict/Run/Rate/Result primary buttons, 1–10 score chips, and pick cards use `heightIn(min = 52.dp)`. Keep the product preset list (「做 5 个深蹲」), not the Stitch recommendation mock or 「做 5 个深呼吸」. Timer finish stays 「我已完成」; home after recording stays 「已记下」. Do not change `MICRO_ACTION_MILLIS`. Micro-action history evidence rows use `heightIn(min = 52.dp)`. Keep `biasCopy` (points, not %) and the source quote 「那张表」. Do not use Stitch 60% / 「这张表」 / a nested 「查看历史记录」 on this page, and do not move `LocalInsights` charts here. Emotion cards 「写下这一张 / 保存卡片 / 它已经过去了」 and word chips use `heightIn(min = 52.dp)`. Keep the 32 concrete words; 「难过 / 很烦」 stay search entries only. Do not change crisis clarification on save.

Settings includes preview controls for relation banners, waiting, and one-thing lock. Previews **must not** call `evaluateAndStore` or `enterCrisisWaiting()`. After closing settings, the real `SafetyState` is unchanged. Export / restore / delete primary buttons and reminder toggle rows use `heightIn(min = 52.dp)`. Keep the four reminder kinds only. Above those toggles, one system-notification row can request `POST_NOTIFICATIONS`. It is not a fifth reminder. Granted says 已允许. Otherwise the button asks, or opens system settings once the dialog will not show again. Refusal still uses the in-app banner. Hosts that do not pass a permission (iOS) hide the row. Under it, 「准时提醒」 opens the system exact-alarm setting when that grant is missing. It is not a fifth reminder. Hide it when the host passes null. Backup still does not restore audio bodies. Do not add a Stitch lockscreen toggle or a doctor PDF export.

Relation hub cards, 「加进清单 / 回血 / 抽干 / 抽一张非社交 / 抽一张社交 / 更轻 / 更紧」 and monitor chips use `heightIn(min = 52.dp)`. Keep default non-social draw, `peoplePleasingHint`, 更轻/更紧, 不是断交, and `leavingCopy`. Do not add Stitch 「开始抽取」, checkbox 防讨好, 「轻盈自然」, or 「系统注意到」. Insights energy/altruism extras are count bars + last-12 sparkline; do not replace `energyCopy` / `altruismFeelCopy` with Stitch % cards.

---

## Layout habits

- Card corner 16.dp, pill buttons ~28.dp / 999.dp
- Bottom tabs: 今天 / 记录 / 洞察 / 我的 (`HomeTab`). Each keeps its 14.sp word (do not drop to 12sp). A 28.dp outlined Material icon sits above it (`today` / `edit_note` / `auto_graph` / `person`). The selected state is a wide pill behind the icon only: it spans that tab, and is not a short capsule wrapped around the two characters or an `AnchorIconWell` circle. The word stays outside the pill. The icon is filled when that shape differs (`today`, `edit_note`, `person`). `auto_graph` is the same path either way. The icon has no content description — the word is the accessible name. `heightIn(min = 48.dp)`. Pad `WindowInsets.navigationBars` inside the bar so the words sit above the gesture handle; the bar background still runs to the screen edge. Settings opens from **我的**, not the top bar.
- Shared `HomeTopBar(onHelp)`: `AnchorAppIcon` (40.dp, no content description) + 「锚点」 heading + circular help (`contentDescription = "此刻需要帮助"`). Both sides are 40.dp so the title stays centered. On the medical-waiting home the circle is omitted and a 40.dp spacer keeps the title centered, because that home's FAB is already 「此刻需要帮助」. Records keeps the circle; it has no help FAB. Do not put the character 「锚」, SOS, or 设置 back on the chrome.
- Records hub is a card page (emotion box + camera log + worry vault), not a nested tab replace of the bottom bar. Hide emotion and worry in medical waiting; journal stays, and the waiting-home 「事实记录」 row opens it. Home scroll is `rememberScrollState()` in `App`, not inside `HomeScreen`. Opening the checklist, guide, or fact log and coming back keeps that position. Camera log write/save use `heightIn(min = 52.dp)`. Evaluative words stay a non-blocking hint with 「挪过去」; do not use Stitch 「感受 / 平静 / 移动」.
- Wave entry is the 192.dp home orb (title + status inside, 「难受的时候点这里」 below). Idle status is 「等待中」. While that round's remaining time is above zero, the status is 「还剩 N 分钟」 (`waveHomeStatus`: round up to the next minute, never 0 while time remains, no seconds). TalkBack uses that status. After the round ends, the status returns to 「等待中」. A remaining time above 12 hours is treated as idle. The disc is the brand wave mark from `design/00-brand-assets/Animated-SVG__9585b03d.html`: pale sage fill (`#D7E6DC`, not the page parchment), three sage waves translating at 4s / 3s / 2.5s, not a flat `primaryContainer` fill. Keep it one-tap reachable in Normal mode.
- Wave recognize: the breathing circle's layout slot is its largest size (`waveBreathScale` stays ≤ 1) and that slot fits the screen, so the circle never paints past the width. The sentence and the line under it stay inside the smallest circle (`waveRecognizeCopyWidth`). 「我看见了」 sits 32.dp under that slot, and the circle plus button are centered together. Do not pin the button to the screen bottom. The body-location step has no breathing circle. On the countdown step the same clearance sits between the breathing circle and both lines of copy (the title above, the body below). The circle is drawn in that slot, not as a full-screen backdrop, so the largest scale does not touch either line. When the timer ends, 「顺手记一张情绪卡」 opens the existing emotion writer and is not required; 「回到今天」 still leaves. Saving or cancelling returns to the ending. The writer is an opaque cover over the ending, the same page background, and it takes the touches; the ending must not show through. The card is the same emotion card. It does not add 给情绪起名 to the home. If any card exists, 记录 shows 情绪标签箱 even during the 14-day lock; medical waiting still hides it.
- Practice-home daily flow (Stitch first-ship Chinese home): the chosen anchor's card sits under the wave, then micro-action | worry-vault side by side (`weight(1f)`, `IntrinsicSize.Min`). The micro-action well matches the worry-vault well on the same row: `tertiaryContainer` at 0.45 and the default `primary` icon tint. Do not use the peach `secondaryContainer` there; that pair is for help and crisis, not a practice. The one-thing lock keeps the wave orb, that row, and the hang FAB; it does not collapse the home to the selected card. The lock sentence sits against that selected card (4.dp), not above the wave. After 14 days, 「再加一件」 appends cards **below** the resident row, in the order added; it does not insert them above 微行动 | 忧虑保险箱. Settings 「预览 14 天已开放（不改选择）」 shows that button without writing `firstAnchorAtMillis` or `addedAnchors`; cards added in the preview disappear on 「关闭预览」. 无损消耗 and 利他微任务 share one `StatusCard`: only the added side is labeled, and both use 「关系与利他」. Only 利他 opens 微小利他 directly; only 无损消耗 opens the hub without 微小利他; both open the hub. No rhythm progress bar, 「温和开始」, or checkmark streak. Rhythm screen save and record circles use `heightIn` / `size` 52.dp. Intro stays 「起得好不好不重要」. Do not add `设置---晨间节律提醒` (fifth notification). Stability reuses `stabilityCopy` and is not a score.

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

## Home-screen widget (Android)

The 「锚点 · 快捷」 widget (`AnchorQuickWidgetReceiver` + `layout/anchor_widget_quick.xml`) is a launcher shortcut, not a dashboard. Rules it established:

- Classic `RemoteViews` + XML, no Glance, no new dependencies; `updatePeriodMillis="0"` — the quick widget has nothing to refresh. A future status widget (slice two) refreshes only from `MainActivity.onResume` + `onUpdate`, never a periodic alarm.
- Receiver is `exported="false"`; tap targets use explicit Intents to `MainActivity` with `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT` and per-button request codes. MainActivity is `singleTop` and routes the `EXTRA_OPEN` extra (`hang|wave|micro`) through `App(openRequest)` — consumption reuses the same `canPractice` guards as home buttons, so medical-waiting and un-added anchors cannot be deep-linked into practice.
- Zero user content on the widget surface: no counts, no streaks, no deadlines, no terracotta red. Palette comes from the same values as `App.kt` (`anchor_widget_*` in `values/colors.xml` + `values-night/`); never tint widgets with the launcher-icon colors.
