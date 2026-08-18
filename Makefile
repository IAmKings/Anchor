# 《锚点 Anchor》项目检查入口
# 用法: make verify —— 运行 PRD 结构校验（文件清单/章节/FR 编号/交叉引用）
.PHONY: verify

verify:
	python3 scripts/verify_prd.py
