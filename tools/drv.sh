#!/usr/bin/env bash
# Drive the connected device: tap, screenshot, and dump app-only log lines.
set -u
export PATH="$PATH:$ANDROID_HOME/platform-tools"
# Git Bash rewrites /sdcard/... into a Windows path before adb sees it, so
# screencap has to be told to keep it. And exec-out into a redirect produced
# a zero-byte file here, so the shot is taken on the device and pulled.
export MSYS_NO_PATHCONV=1
OUT="C:/Users/mrrob/Desktop/Items/zcode/qr-code-app/shots"
mkdir -p "$OUT"

shot() {
  adb shell screencap /sdcard/qs-shot.png >/dev/null 2>&1
  adb pull /sdcard/qs-shot.png "$OUT/$1.png" >/dev/null 2>&1
  echo "shot: $1.png ($(wc -c < "$OUT/$1.png") bytes)"
}
tap()  { adb shell input tap "$1" "$2"; sleep "${3:-1.5}"; }
typ()  { adb shell input text "$1"; sleep 0.5; }
back() { adb shell input keyevent 4; sleep "${1:-1.5}"; }
swipe(){ adb shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"; sleep 1; }

# App-scoped diagnostics: our package plus anything that looks like a crash.
errs() {
  adb logcat -d -v brief 2>/dev/null \
    | grep -E 'AndroidRuntime|FATAL EXCEPTION|com\.quickscan\.debug|qs_?E/|StrictMode' \
    | grep -vE 'SurfaceFlinger|WindowManager|ActivityTaskManager|GameManager|GamePkg|GameSDK|ApplicationPolicy|CodecSolution|MARsPolicy|PackageManager|DSS OFF|PhoneWindow|WifiTransport'
}

clear() { adb logcat -c 2>/dev/null; }