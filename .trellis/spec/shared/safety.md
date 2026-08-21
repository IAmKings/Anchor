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

`keywordHit` without `clarification` is the only path that does **not** move state. Providing `clarification` when `keywordHit` is false is illegal.

Item 9 crisis **skips GAD-7** in the first-run UI (`shouldSkipGad7`). The policy still requires a 7-length GAD list; onboarding fills zeros when skipped.

---

## Medical waiting vs practice lock

These are different mechanisms. Do not merge them.

- **Medical waiting** (`SafetyMode.MedicalWaiting`): hide practice entries; keep help, medical guide, somatic checklist, and fact-only camera log. Copy: pause is not punishment (`medicalWaitingBody` in `OnboardingCopy.kt`).
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
