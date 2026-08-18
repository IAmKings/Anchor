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

