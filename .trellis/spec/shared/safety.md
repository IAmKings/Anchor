# Safety Kernel

> Crisis, scoring, and medical-waiting rules are product-critical. Do not reimplement them in UI.

---

## Scope / trigger

Any change to PHQ-9 / GAD-7, crisis copy, hotlines, waiting-mode visibility, or "one thing" practice lock.

Owner: `SafetyPolicy` in `shared/src/commonMain/kotlin/com/anchor/app/safety/SafetyPolicy.kt`. Tests: `SafetyPolicyTest.kt`.

---

## Signatures

```kotlin
object SafetyPolicy {
    fun evaluate(
        phq9: List<Int>,          // size 9, each 0..3
        gad7: List<Int>,          // size 7, each 0..3
        completedAtMillis: Long,  // >= 0
        previous: SafetyState = SafetyState(),
        keywordHit: Boolean = false,
        clarification: Clarification? = null,  // only when keywordHit
        repeatedLowSelfEvaluation: Boolean = false,
    ): SafetyOutcome
}

data class SafetyState(
    val mode: SafetyMode = SafetyMode.Normal,
    val firstLowAssessmentAtMillis: Long? = null,
)
```

Persistence goes through `AnchorStore.evaluateAndStore`. Direct `enterCrisisWaiting()` exists for in-app crisis entry without a new scale (e.g. help flow); it writes `MedicalWaiting` with `firstLowAssessmentAtMillis = null`.

---

## Contracts (evaluation order)

`evaluate` returns on the first match:

| Order | Condition | Action | Next state |
|------|-----------|--------|------------|
| 1 | `phq9[8] > 0` (item 9) | `CrisisGuidance` | `MedicalWaiting` |
| 2 | `keywordHit && clarification == null` | `AskClarification` | **previous unchanged** |
| 3 | `keywordHit && clarification != NotSelf` (`Self` or `Uncertain`) | `CrisisGuidance` | `MedicalWaiting` |
| 4 | PHQ ≥ 15 or GAD ≥ 15 | `MedicalWaiting` | `MedicalWaiting` |
| 5 | else | `Continue`, or `ClinicalReview` if `repeatedLowSelfEvaluation` | `recoverIfEligible(previous)` |

Bands (do not "simplify"):

- PHQ-9: 0–4 Minimal, 5–9 Mild, 10–14 Moderate, 15–19 ModeratelySevere, 20–27 Severe.
- GAD-7: 0–4 Minimal, 5–9 Mild, 10–14 Moderate, 15–21 Severe (no ModeratelySevere).

Hysteresis (`recoverIfEligible`): leaving `MedicalWaiting` requires **two** low assessments (PHQ ≤ 9 **and** GAD ≤ 9) whose timestamps are **≥ 7 days** apart. A high score in between resets to waiting without a first-low timestamp. `firstLowAssessmentAtMillis == null` means "this is the first low".

`stillWaitingKind` names the step when the outcome is still waiting: first low, low but under 7 days, not yet both ≤ 9, or still ≥ 15. It does not change `evaluate`. A screen must not re-derive that step from raw answers.

`keywordHit` without `clarification` is the only path that does **not** move state. Providing `clarification` when `keywordHit` is false is illegal.

Item 9 crisis **skips GAD-7** in the first-run UI (`shouldSkipGad7`). The policy still requires a 7-length GAD list; onboarding fills zeros when skipped.

---

## Medical waiting vs practice lock

These are different mechanisms. Do not merge them.

- **Medical waiting** (`SafetyMode.MedicalWaiting`): hide practice entries; keep help, medical guide, somatic checklist, and fact-only camera log. The waiting-home 「事实记录」 row opens that log even when another anchor holds the 14-day lock (`factLogVisible`). Do not gate it with `practiceVisible` alone. Copy: pause is not punishment (`medicalWaitingBody` in `OnboardingCopy.kt`).
- **One-thing lock**: after first-anchor choice, only that practice is visible for 14 days (`REASSESSMENT_INTERVAL_MILLIS`). `firstAnchorAtMillis == null` (legacy profiles) is treated as unlocked. Helpers: `additionalPracticeUnlocked`, `practiceVisible` in `OnboardingCopy.kt`.

Wave waiting stays reachable from the home orb even when other practices are locked, unless medical waiting is active.

Settings may **preview** waiting / relation / lock states without writing `SafetyState`. Do not persist preview toggles.

---

## Crisis resources

Hotlines are code, not design-file numbers.

- Compact help: `crisisResource(region, youth)` in `CrisisResources.kt`.
- Named cards for the guide: `medicalGuideContent(region, youth)`.
- Mainland youth vs adult: 12355 vs 12356.
- Numbers follow PRD §10.4 (12356 / 1925 / 18111 / 988 / …). Stitch mocks that show other numbers are wrong.

Do not fetch or "verify" numbers on a network. If a number is wrong, change the constants and tests (`CrisisResourcesTest`).

---

## Safety page stack

Crisis, guide, checklist, and the guide's journal are one stack in `App`: `safetyStack: List<SafetyPlace>` (`SafetyStack.kt`). They are not independent booleans. A child must not clear the parent flag to become visible.

| Place | How it opens | Back |
|-------|----------------|------|
| `Help` | Push from home, records, or settings. `openCrisisFromText` **replaces** the stack with Help (worry, emotion, camera log, hang sheet, records, settings). | Pop one. The opener's flag is still set, so home / records / settings returns. |
| `Guide(preview)` | Push from Help, home, or settings preview. First-run, assessment preview, and reassessment **close that flow** and set a new root `listOf(Guide)`, not a push. | Pop one. Settings preview keeps `settingsVisible`. |
| `Checklist` | Push from Help, Guide, or home. Same new-root rule as Guide when opened from an assessment flow. | Pop one. |
| `Journal` | Push only from the guide's 「打开记录」. | Pop back to the guide. |

`pushSafety` appends. `popSafety` drops the last entry; an empty stack stays empty. System back and `AnchorBack` both call that pop.

The records hub's own camera log is still `cameraLogVisible`, and that branch is checked **after** the stack. Do not route it through `SafetyPlace.Journal`.

Wave, worry, insights, and the other full-screen flags still return before the stack. `openCrisisFromText` clears the ones that would hide Help.

---

## Dialing

Parsing lives in `PhoneDial.kt`. Do not copy it into a screen.

- Split on `/` `、` `；` `;` `或`. Do not split on spaces: `2382 0000` is one number.
- A token is dialable only when it contains no letters (`isLetter`, so Chinese counts) and, after dropping spaces and hyphens, is an optional `+` plus 3–15 digits.
- `phoneTelUri` returns `tel:` plus digits, keeping one leading `+`. `010-82951332` → `tel:01082951332`. `+86 10 55` → `tel:+861055`.
- `当地紧急电话`, `请查询当地心理援助资源`, and `NHS 111 转 2` are not buttons. A dialable piece after a separator still is (`116 123`).
- `GuideHotline.phoneNumbers()` is `dialableNumbers(number) + dialableNumbers(detail)`, distinct. `note()` is `detail` only when `detail` itself has no dialable number (北京 `800-810-1117` is a second target, not a note).
- If the whole string is dialable, do not also print it as plain text. Otherwise keep the prose and add a button per dialable piece.
- No dialable piece means no button. Do not invent one, and do not call `onClose` / `onFinish` from a dial control.

The host opens the system dialer. Android: `Intent.ACTION_DIAL`. iOS: `UIApplication.openURL` with the `tel:` URL. Do not use `canOpenURL` (that is what needs `LSApplicationQueriesSchemes`). No `CALL_PHONE`. The safety page stays open. `ActivityNotFoundException` or a missing dialer leaves the number on the page. The user confirms the call. Do not fetch numbers.

---

## Validation & error matrix

| Input | Result |
|-------|--------|
| PHQ length ≠ 9 or GAD length ≠ 7 | `IllegalArgumentException` `"PHQ-9 必须有 9 个答案"` / GAD equivalent |
| Any item outside 0..3 | `"…每题分数必须在 0..3"` |
| `completedAtMillis < 0` | `"评估时间不能为负数"` |
| `clarification != null && !keywordHit` | `"没有关键词命中时不应提供澄清结果"` |
| PHQ total outside 0..27 (via `phqBand`) | `error("PHQ-9 分数必须在 0..27")` |

---

## Good / base / bad

- **Good**: item 9 = 1 even with `clarification = NotSelf` still returns `CrisisGuidance` and waiting (`phqItemNineAlwaysTriggersCrisis`).
- **Base**: keyword hit → `AskClarification` and unchanged state; `NotSelf` then continues.
- **Bad**: treating `Uncertain` as safe. Policy maps it to `CrisisGuidance`.

---

## Tests required

- Band boundaries: every threshold in `scoreBandsCoverEveryBoundary`.
- Item 9, keyword clarification matrix, either-scale-at-15, 6-day vs 7-day recovery.
- Copy: crisis result badge `"需要立即支持"`, waiting `"中度至重度"` (`BaselineAssessmentTest`).
- Hotline mapping per region and youth flag (`CrisisResourcesTest`).
- Stack push/pop, including journal back to the guide and pop on an empty stack (`SafetyStackTest`).
- Dial split, `tel:` normalization, prose rejection, and Beijing / 12356 note (`PhoneDialTest`).

When changing hysteresis or bands, extend `SafetyPolicyTest` first. Do not "fix" waiting by writing `SafetyState()` from a screen.

---

## Wrong vs correct

#### Wrong
```kotlin
if (phq9.sum() > 10) store.saveUserProfile(profile.copy(/* ... */))
```
or a composable `if (score >= 15) MedicalWaitingHome()`.

#### Correct
```kotlin
val outcome = store.evaluateAndStore(input)
when (outcome.action) { is SafetyAction.CrisisGuidance -> /* HelpNow */ ... }
```

#### Wrong
Delete `SafetyState` to "reset" a tester, or skip waiting so a demo can show practices.

#### Correct
Use two low assessments ≥7 days apart, or the settings **preview** flags that do not persist.

---

## Do not

- Soften crisis copy, hide hotlines, or add an in-app chatbot "instead of calling".
- Drive waiting visibility from PHQ band text in UI instead of `SafetyMode`.
- Change 7-day hysteresis or 15-point waiting threshold without an explicit product decision and test updates.
- Mix design-file hope numbers into `CrisisResources`.
