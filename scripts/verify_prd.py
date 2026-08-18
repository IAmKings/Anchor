"""PRD 结构校验脚本 — 项目检查工具（verify 用）。

用法: python3 scripts/verify_prd.py
检查项: 交付文件清单 / 章节 ## 1..12 / FR-1.1..FR-7.10 定义完整性 /
        代码块围栏配对 / v1.3 关键修订落盘 / 关键交叉引用。
退出码: 全部通过为 0, 任一失败为 1。
"""
import re
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
PRD = ROOT / "PRD.md"

checks = []


def check(name, ok, detail=""):
    checks.append((name, ok, detail))


# 1. 交付文件清单
for f, label in [
    (PRD, "PRD.md"),
    (ROOT / "CONTEXT.md", "CONTEXT.md"),
    (ROOT / "docs" / "FEASIBILITY_CHECK.md", "docs/FEASIBILITY_CHECK.md"),
    (ROOT / "docs" / "adr" / "0001-local-only-no-account.md", "adr/0001"),
    (ROOT / "docs" / "adr" / "0002-compose-multiplatform.md", "adr/0002"),
    (ROOT / "docs" / "adr" / "0003-anti-kpi-metrics.md", "adr/0003"),
]:
    check(f"文件存在: {label}", f.is_file())

text = PRD.read_text(encoding="utf-8")

# 2. 一级章节 1..12
for i in range(1, 13):
    check(f"章节 ## {i} 存在", re.search(rf"^## {i}\. ", text, re.M) is not None)

# 3. FR 定义完整性 1.1..7.10
fr_sections = [(1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 10)]
missing = []
for s, n in fr_sections:
    for j in range(1, n + 1):
        if not re.search(rf"FR-{s}\.{j}\b", text):
            missing.append(f"FR-{s}.{j}")
check("FR-1.1..FR-7.10 定义完整", not missing, ",".join(missing))

# 4. 代码块围栏配对
fences = text.count("```")
check("代码块围栏偶数配对", fences % 2 == 0, str(fences))

# 5. v1.3 关键修订落盘
for kw in [
    "v1.3 修订说明（严格零收集）",
    "首启自报",
    "计时器后台行为",
    "通知关闭兜底",
    "零埋点上报（严格零收集",
]:
    check(f"v1.3 修订落盘: {kw}", kw in text)

# 6. 关键交叉引用
for ref in ["ADR-0003", "§10.4", "§12.7", "§6.5", "FR-7.9", "FR-5.3",
            "12356", "988", "1925"]:
    check(f"交叉引用: {ref}", ref in text)

failed = [c for c in checks if not c[1]]
for name, ok, detail in checks:
    print(("PASS" if ok else "FAIL"), name, detail)
print(f"\n{len(checks) - len(failed)}/{len(checks)} passed")
sys.exit(1 if failed else 0)
