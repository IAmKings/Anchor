#!/usr/bin/env bash
# 锚点发版流程（固化自 0.1.4/0.1.5 两轮手动发布）：
#   1. 版本号（gradle.properties，code 自动 +1）
#   2. 法务页生效日期（terms/privacy）
#   3. README 与 site 的版本号/下载链接同步
#   4. 提交 + tag + 推送 → 触发 GitHub Workflow 双渠道发布
#   5. 转正式版 + Latest + 追加更新亮点
#   6. 部署 legal 与介绍站（Cloudflare Pages）
#   7. 本机构建并安装 debug 包（设备连接时）
#
# 用法:
#   ./scripts/release.sh <版本号> [亮点文件.md]
#   DRY_RUN=1 ./scripts/release.sh <版本号>        # 只打印将执行的步骤
#
# 例:
#   ./scripts/release.sh 0.1.6
#   ./scripts/release.sh 0.1.6 release-notes.md
set -euo pipefail

VERSION="${1:-}"
HIGHLIGHTS_FILE="${2:-}"
DRY_RUN="${DRY_RUN:-0}"
REPO="IAmKings/Anchor"

if [ -z "$VERSION" ]; then
  echo "用法: ./scripts/release.sh <版本号 X.Y.Z> [亮点文件.md]"
  exit 1
fi
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "版本号格式应为 X.Y.Z，收到: $VERSION"; exit 1; }
TAG="v$VERSION"

run() {
  if [ "$DRY_RUN" = "1" ]; then
    echo "[dry-run] $*"
  else
    "$@"
  fi
}

# ---------- 前置检查 ----------
if [ "$DRY_RUN" != "1" ] && [ -n "$(git status --porcelain | grep -v zcodeignore)" ]; then
  echo "✗ 工作区不干净，先提交或暂存（git status）"; exit 1
fi
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  echo "✗ tag $TAG 已存在"; exit 1
fi
PREV_CODE=$(grep '^anchorVersionCode=' gradle.properties | cut -d= -f2)
PREV_VERSION=$(grep '^anchorVersionName=' gradle.properties | cut -d= -f2)
NEXT_CODE=$((PREV_CODE + 1))
DATE_CN=$(python3 -c "from datetime import date; t = date.today(); print(f'{t.year} 年 {t.month} 月 {t.day} 日')")
echo "发版 $PREV_VERSION($PREV_CODE) → $VERSION($NEXT_CODE)，生效日期 $DATE_CN"

# ---------- 1-3. 文件同步（版本号/法务日期/README/site） ----------
if [ "$DRY_RUN" != "1" ]; then
  sed -i '' "s/^anchorVersionCode=.*/anchorVersionCode=$NEXT_CODE/" gradle.properties
  sed -i '' "s/^anchorVersionName=.*/anchorVersionName=$VERSION/" gradle.properties
  sed -i '' "s/生效日期：[0-9]\{4\} 年 [0-9]* 月 [0-9]* 日。/生效日期：${DATE_CN}。/" legal/terms.html legal/privacy.html
  sed -i '' "s/当前版本 \`$PREV_VERSION\`/当前版本 \`$VERSION\`/" README.md
  sed -i '' "s/$PREV_VERSION/$VERSION/g" site/index.html
  grep -q "anchorVersionName=$VERSION" gradle.properties && grep -q "$VERSION" site/index.html && grep -q "$DATE_CN" legal/terms.html \
    || { echo "✗ 文件同步校验失败"; exit 1; }
  echo "✓ 版本号/法务日期/README/site 已同步"
else
  echo "[dry-run] gradle.properties → $VERSION($NEXT_CODE); legal 日期 → $DATE_CN; README/site → $VERSION"
fi

# ---------- 4. 提交 + tag + 推送 ----------
run git add -A -- ':!.zcodeignore'
run git commit -m "chore: 内测版本升到 ${VERSION}（code ${NEXT_CODE}）；法务页日期与 README/site 版本信息同步"
run git tag "$TAG"
run git push origin master
run git push origin "$TAG"

# ---------- 5. 等待 CI 双渠道发布 ----------
if [ "$DRY_RUN" = "1" ]; then
  echo "[dry-run] 等待 workflow 发布 $TAG 并验证双渠道资产"
else
  echo "等待 GitHub Actions（发布 $TAG）…"
  sleep 20
  RUN_ID=$(gh run list --workflow=internal-release.yml --limit 5 --json databaseId,headBundle --jq ".[] | select(.headBranch == \"$TAG\") | .databaseId" | head -1)
  [ -n "$RUN_ID" ] || { echo "✗ 未找到 $TAG 的 workflow run，请手动检查 gh run list"; exit 1; }
  if ! gh run watch "$RUN_ID" --exit-status > /dev/null 2>&1; then
    echo "✗ workflow 失败（既往先例：镜像源瞬时 502）。重试: gh run rerun $RUN_ID"; exit 1
  fi
  ASSETS=$(gh release view "$TAG" -R "$REPO" --json assets --jq '[.assets[].name] | join(",")')
  echo "$ASSETS" | grep -q "anchor-internal-$VERSION.apk" && echo "$ASSETS" | grep -q "anchor-internal-play-$VERSION.apk" \
    || { echo "✗ 双渠道资产缺失: $ASSETS"; exit 1; }
  echo "✓ 双渠道资产就绪: $ASSETS"
fi

# ---------- 5b. 转正式 + Latest + 亮点 ----------
run gh release edit "$TAG" -R "$REPO" --prerelease=false
run gh release edit "$TAG" -R "$REPO" --latest
if [ -n "$HIGHLIGHTS_FILE" ] && [ -f "$HIGHLIGHTS_FILE" ]; then
  if [ "$DRY_RUN" = "1" ]; then
    echo "[dry-run] 追加亮点: $HIGHLIGHTS_FILE"
  else
    BODY=$(gh release view "$TAG" -R "$REPO" --json body --jq .body)
    printf '%s\n\n**%s 更新亮点**\n\n%s\n' "$BODY" "$VERSION" "$(cat "$HIGHLIGHTS_FILE")" > /tmp/release-body.md
    gh release edit "$TAG" -R "$REPO" --notes-file /tmp/release-body.md
    echo "✓ 亮点已追加"
  fi
fi

# ---------- 6. 部署 legal 与 site ----------
run env CI=true npx wrangler pages deploy legal --project-name=anchor-legal --branch=main --commit-dirty=true
run env CI=true npx wrangler pages deploy site --project-name=anchor-125457 --branch=main --commit-dirty=true

# ---------- 7. 本机 debug 装机（设备连接时） ----------
if [ "$DRY_RUN" = "1" ]; then
  echo "[dry-run] 构建 debug 并尝试 adb 装机"
else
  ./gradlew :androidApp:assembleStandardDebug --console=plain -q
  if adb get-state >/dev/null 2>&1; then
    adb install -r androidApp/build/outputs/apk/standard/debug/androidApp-standard-debug.apk
  else
    echo "（无已连接设备，跳过本机装机）"
  fi
fi

echo "✅ ${VERSION} 发布完成（tag $TAG 已转正式 + Latest；legal/site 已部署）"
