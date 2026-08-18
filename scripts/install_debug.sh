#!/bin/sh
# Install Debug APKs with replace-only. Never uninstall com.anchor.app.
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
APP_APK="$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
TEST_APK="$ROOT/androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk"

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
