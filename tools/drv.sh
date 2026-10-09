#!/usr/bin/env bash
# Drive the connected device: tap, screenshot, and dump app-only log lines.
set -u
export PATH="$PATH:$ANDROID_HOME/platform-tools"
OUT="C:/Users/mrrob/Desktop/Items/zcode/qr-code-app/shots"
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null; echo "shot: $1.png"; }
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