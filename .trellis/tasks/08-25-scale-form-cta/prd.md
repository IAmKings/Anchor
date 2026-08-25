# 量表页选项加高并固定提交

## Goal

PHQ-9 / GAD-7 作答页的选项更好点，提交钮钉在底部。题目、选项文案、第 9 题注和 `SafetyPolicy` 都不改。

## Background

`ScaleForm` 被首启 PHQ、GAD 和复评共用。选项目前两列、`padding(vertical = 10.dp)`，提交和免责跟题目一起滚走。

Stitch PHQ 稿是每题四个 radio，底部「提交评估」+ 免责。窄屏两列更适合大字号，不改成四列。

`allAnswered` 仍控制能否提交。第 9 题 `highlightLast` + `item9CrisisNote` 保留。GAD 在第 9 题 >0 时仍跳过（`shouldSkipGad7`）。

## Requirements

1. 四个选项芯片 `heightIn(min = 52.dp)`，字号 ≥14sp，仍两列。
2. 「提交评估」钉在底部，≥52.dp；未答完不可点。
3. `scaleDisclaimer`、`scaleSource` 仍可见（可放提交条上方或列表末）。
4. 不改编题、`assessmentChoices`、计分、第 9 题跳转。
5. 返回仍在顶部。

## Out of scope

- 改 Pfizer 题目或选项用词
- 一次一题向导
- `SafetyPolicy`、热线
- 复评趋势页

## Acceptance Criteria

- [ ] 选项最小高度 52.dp。
- [ ] 提交钮在底部固定；`allAnswered` 为假时不可点。
- [ ] `phq9Questions.last()` 仍是「有不如死掉或伤害自己的念头」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Key decisions

- 选项保持两列，不跟稿子宽屏四列。
- 提交条交互跟轻度/等待/危机结果页同一习惯。
