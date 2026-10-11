# 《锚点 Anchor》项目检查入口
# 用法: make verify —— 运行 PRD 结构校验（文件清单/章节/FR 编号/交叉引用）
#       make install-debug —— 只覆盖安装 Debug 包，不卸载
.PHONY: verify install-debug install-debug-test

verify:
	python3 scripts/verify_prd.py
	python3 scripts/verify_privacy.py

install-debug:
	sh scripts/install_debug.sh

install-debug-test:
	sh scripts/install_debug.sh --with-test


# 一键发版：版本号/法务日期/README+site 同步 → tag → CI 双渠道 → 转正+亮点 → 部署 legal+site → 本机装机
# 用法: make release V=0.1.6 HIGHLIGHTS=release-notes.md   （先用 DRY_RUN=1 make release V=0.1.6 预演）
release:
	DRY_RUN=$(DRY_RUN) ./scripts/release.sh $(V) $(HIGHLIGHTS)
