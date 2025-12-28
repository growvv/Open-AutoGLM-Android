# Deploy / Smoke Test Notes

This folder documents how to build, install, and smoke-test **Open-AutoGLM-Android**, plus the recent configuration/code changes and issues encountered during CLI-based deployment.

## Prerequisites

- Android SDK installed and `adb` available on PATH
- A real Android device connected and visible via `adb devices`
- Project root: `Open-AutoGLM-Android/`

Optional:
- To use the “silent keep-alive” toggle (writing secure settings), grant:
  - `adb shell pm grant com.example.open_autoglm_android android.permission.WRITE_SECURE_SETTINGS`

## Backend / Settings (vLLM OpenAI-compatible)

Current default settings in the app:
- **Base URL**: `http://47.99.92.117:28100/v1`
- **API Key**: optional (leave empty to send no `Authorization` header)
- **Model Name**: `autoglm-phone-9b`

Notes:
- `http://47.99.92.117:28100/v1/models` is expected to be accessible without an API key in this environment.
- Cleartext HTTP is enabled in the app manifest to support `http://` base URLs.

## Build

From repo root:

```bash
./gradlew :app:assembleDebug
```

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

## Install / Reinstall (device)

Pick the target device:

```bash
adb devices
export ANDROID_SERIAL=<device-serial>
```

Install or reinstall:

```bash
adb -s "$ANDROID_SERIAL" install -r -t app/build/outputs/apk/debug/app-debug.apk
```

Optional: clear app data (fresh start):

```bash
adb -s "$ANDROID_SERIAL" shell pm clear com.example.open_autoglm_android
```

Launch:

```bash
adb -s "$ANDROID_SERIAL" shell am start -n com.example.open_autoglm_android/.MainActivity
```

## Smoke Tests (on device)

Run instrumentation tests on the connected device:

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

Test reports:
- HTML: `app/build/reports/androidTests/connected/debug/index.html`
- JUnit XML: `app/build/outputs/androidTest-results/connected/debug/`

What’s covered:
- vLLM `/v1/models` reachable without API key
- `ModelClient` omits `Authorization` header when API key is blank
- Settings screen shows default Base URL / Model Name

## Recent Code/Config Changes (summary)

### Backend defaults + API key optional
- Default Base URL and Model Name updated in `PreferencesRepository`
- `ModelClient` only sets `Authorization: Bearer ...` when API key is non-blank (and not `"EMPTY"`)
- Settings UI defaults updated accordingly

### Accessibility “enabled” state fix
Problem: the app sometimes showed “not enabled” even though the user already enabled the accessibility service.

Fix:
- Accessibility enabled detection now **prefers** `AccessibilityManager.getEnabledAccessibilityServiceList(...)` and falls back to parsing `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`.
- Settings screen distinguishes:
  - not enabled
  - enabled but service still connecting
  - enabled and service running
- Chat execution no longer rejects immediately if the service instance is not yet available; it waits briefly for connection.

### Max Steps setting
- Added `Max Steps (1~500)` in Settings
- Persisted via DataStore
- Task loop uses the configured value instead of a hard-coded 50

### Conversation status indicator
- Added per-conversation status: `未开始 / 进行中 / 完成 / 中止 / 结束`
- Stored in Room (`conversations.status`) with DB migration `v1 -> v2`
- Updated automatically when a task starts/finishes/is stopped/errors/max-steps reached
- Displayed in the conversation drawer (next to last updated time)

## Issues Encountered (and resolutions)

### Kotlin compile daemon instability (CLI builds)
- Symptom: sporadic Kotlin compilation crashes during Gradle runs
- Resolution: set Kotlin compiler execution strategy to in-process:
  - `gradle.properties`: `kotlin.compiler.execution.strategy=in-process`

### Proxy settings breaking dependency downloads
- Symptom: Gradle dependency resolution failed due to empty proxy entries
- Resolution: remove empty proxy entries from `gradle.properties`

### Instrumentation crash due to Shizuku unbind
- Symptom: tests crashed on activity destroy when Shizuku binder was not available
- Resolution: wrap `Shizuku.unbindUserService(...)` with `try/catch IllegalStateException`

### UI test navigation flakiness
- Symptom: tests couldn’t reliably find Settings tab by localized content description
- Resolution: add stable Compose `testTag`s to navigation + settings fields and use them in tests

