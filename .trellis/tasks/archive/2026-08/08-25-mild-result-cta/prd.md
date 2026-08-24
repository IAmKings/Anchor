# 轻度评估结果页底部固定选锚点

## Goal

轻度结果页只负责报告分数并送去「选择第一个锚点」。CTA 钉在底部，和危机/等待结果页同一交互习惯。

## Background

Stitch `评估结果---轻度引导-(中文版)` 在结果页上直接选「穿好鞋走到楼下」等微行动再「确认并开始」。产品规则是一次一件、先选锚点（6 个 P0 + P1），不能在结果页把选择收成三个微行动。

现 `MildResult`：分数环、`mildInsight`、PHQ/GAD、免责、页内「选择第一个锚点」。

## Requirements

1. 「选择第一个锚点」钉在底部，≥52.dp，进入已有 `FirstAnchorChoice`。
2. 页内提示一次一件和 14 天（用现有 `firstAnchorCommitHint`），**不要**微行动单选列表。
3. 保留分数环、`mildInsight`、PHQ/GAD 行、`scaleDisclaimer`、`scaleSource`。
4. 不改 `SafetyPolicy`、分档文案。

## Acceptance Criteria

- [ ] 轻度结果页没有「穿好鞋走到楼下」等预选微行动。
- [ ] 底部固定「选择第一个锚点」。
- [ ] 可见「选定后我们将固定练习 14 天」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。
