#!/bin/sh
# Install Debug APKs with replace-only. Never uninstall com.anchor.app.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
# 渠道拆分后，默认渠道 standard 的产物路径带 standard 前缀。
APP_APK="$ROOT/androidApp/build/outputs/apk/standard/debug/androidApp-standard-debug.apk"
TEST_APK="$ROOT/androidApp/build/outputs/apk/standard/androidTest/debug/androidApp-standard-debug-androidTest.apk"

if [ ! -f "$APP_APK" ]; then
  echo "missing $APP_APK" >&2
  exit 1
fi

adb install -r "$APP_APK"
if [ "${1:-}" = "--with-test" ]; then
  if [ ! -f "$TEST_APK" ]; then
    echo "missing $TEST_APK" >&2
    exit 1
  fi
  adb install -r "$TEST_APK"
fi
adb shell pm path com.anchor.app
