# Security & Privacy Audit Report (Static Review)

Date: 2025-12-27  
Project: `Open-AutoGLM-Android`  
Audited revision: `b4bc66e74981e8403d7831077dd88980e8a72b11`  
Method: manual static review (source + manifest + build config). No dynamic traffic capture.

## Executive Summary

This project is an on-device automation agent using **AccessibilityService + screenshots** and an **OpenAI-compatible HTTP backend**. By design, it can observe other apps’ UI content and transmit screenshots to the configured model backend to decide actions. This is inherently high-risk for privacy.

### Key results

- **No evidence of a hidden backdoor endpoint** (no extra hard-coded domains/IPs beyond the model backend base URL and a Shizuku website link).
- **High privacy risk is present by design** (screen capture + UI automation + optional IME), and there are several **implementation choices that amplify the risk**:
  - **HTTP request/response body logging is enabled** (can leak screenshots + prompts + potentially API keys to `logcat`).
  - **IME logs the exact text it inputs** (could leak passwords/OTP/PII to logs).
  - **Secrets and chat history are stored unencrypted and are eligible for backup** (`allowBackup=true`).
  - This fork currently defaults the model backend to `http://47.99.92.117:28100/v1` (data may be sent to this remote host by default once a user runs a task).

## Scope

Reviewed:
- Android manifest, exported components, and permissions.
- Network code (`Retrofit`/`OkHttp`) and model request content.
- Storage of API key, conversation history, screenshots.
- Command execution / privilege elevation paths (Shizuku / root).
- Third-party dependencies declared in Gradle.

Not reviewed:
- The remote model backend itself (server-side logging/retention/policies).
- Supply-chain of Gradle/Maven repositories beyond declared dependencies.
- Binary artifacts not in repo (none found besides `gradle-wrapper.jar`).

## Threat Model (what “backdoor / exfil” means here)

Potential issues considered:
- Hidden network endpoints sending data without user intent.
- Exfiltration of passwords/API keys/PII (via network, logs, storage, backups).
- Covert data collection (device identifiers, contacts, location, clipboard scraping).
- Remote code execution / dynamic code loading.
- Privilege escalation abuse (root/Shizuku) beyond stated purpose.

## App Capabilities & Attack Surface

### Declared permissions (high-level)
Source: `app/src/main/AndroidManifest.xml`

- Network: `INTERNET`, `ACCESS_NETWORK_STATE`
- Broad app visibility: `QUERY_ALL_PACKAGES`
- Storage (very broad): `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`, plus `requestLegacyExternalStorage=true`
- Overlay: `SYSTEM_ALERT_WINDOW`
- Boot start: `RECEIVE_BOOT_COMPLETED`
- Foreground service: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`
- Privileged settings (special): `WRITE_SECURE_SETTINGS` (normally only via adb/root/Shizuku)

### Exported components
Source: `app/src/main/AndroidManifest.xml`

- `MainActivity` exported (launcher)
- Accessibility service exported **with** `BIND_ACCESSIBILITY_SERVICE` (system-only binding)
- Input method service exported **with** `BIND_INPUT_METHOD` (system-only binding)
- Boot receiver exported and runs on boot events
- Quick Settings tile exported with `BIND_QUICK_SETTINGS_TILE`
- Shizuku provider exported (from Shizuku library)

## Data Flows (what leaves the device)

### Model backend requests (intended behavior, but privacy-sensitive)
Source: `app/src/main/java/com/example/open_autoglm_android/network/ModelClient.kt`

- Sends `POST {baseUrl}/chat/completions`
- Request includes:
  - user prompt text
  - JSON `{"current_app": "<package>"}` text
  - **screenshot** encoded as `data:image/jpeg;base64,...` in `image_url` content items
- If API key is provided, it sets `Authorization: Bearer <key>` header.

Implication: any sensitive information visible on screen (including other apps) can be transmitted to the backend when tasks run.

### Local persistence (sensitive)
- API key + base URL + model name + other settings are stored in `DataStore` preferences (plaintext).
  - Source: `app/src/main/java/com/example/open_autoglm_android/data/PreferencesRepository.kt`
- Chat history is stored in Room (`app_database`) including prompt/response text.
  - Source: `app/src/main/java/com/example/open_autoglm_android/data/database/SavedChatMessage.kt`
- Screenshots (with action markers) are written to app external files:
  - Source: `app/src/main/java/com/example/open_autoglm_android/util/BitmapUtils.kt`
  - Path: `context.getExternalFilesDir("action_images")/...`

## Findings

Severity scale used: **Critical / High / Medium / Low / Informational**.

### F-01 (Critical): HTTP logging interceptor logs full bodies (screenshots, prompts, and possibly API keys)

Evidence:
- `app/src/main/java/com/example/open_autoglm_android/network/ModelClient.kt#L29` enables:
  - `HttpLoggingInterceptor.Level.BODY` (see `#L30-32`)
  - It is added to `OkHttpClient` unconditionally (see `#L34-36`)
- The request body contains base64-encoded screenshots and prompt text.
- If API key is set, `Authorization` header is attached (see `#L40-43`).

Impact:
- Sensitive content can be written to `logcat`, which may be accessible via:
  - local debugging, USB debugging sessions
  - device logs, OEM log collection, crash reports, or other log collectors
- Screenshots may contain passwords, OTPs, banking data, messages, photos, etc.
- API key can be exposed in logs unless redacted.

Recommendation:
- Disable HTTP body logging in release builds and preferably in debug too.
- At minimum:
  - set `level = NONE` for release builds
  - redact `Authorization` header (`loggingInterceptor.redactHeader("Authorization")`)
  - avoid logging request bodies for image data

### F-02 (High): IME logs full text being typed (possible password/OTP exposure)

Evidence:
- `app/src/main/java/com/example/open_autoglm_android/service/MyInputMethodService.kt#L44`
  - `Log.d(TAG, "正在通过 IME 输入文本: $text")`

Impact:
- If the agent types into a password field or OTP field, the plaintext value is recorded in logs.

Recommendation:
- Remove this log line or mask sensitive content.
- Consider adding explicit detection/avoidance for password fields and never logging input text.

### F-03 (High): Default backend points to a remote host (data exfiltration risk by default)

Evidence:
- `app/src/main/java/com/example/open_autoglm_android/data/PreferencesRepository.kt` sets:
  - `DEFAULT_BASE_URL = "http://47.99.92.117:28100/v1"`

Impact:
- Users who enable accessibility and run a task may unknowingly send screenshots/prompts to a third-party server if they do not change settings.

Recommendation:
- Default base URL should be empty or a clearly local placeholder and require explicit user confirmation before first network transmission.
- Add a “privacy confirmation” gate that explains:
  - what data is sent (screenshots, current app, prompt)
  - where it is sent (base URL)
  - retention risks

### F-04 (High): Cleartext HTTP enabled globally

Evidence:
- `app/src/main/AndroidManifest.xml#L39` sets `android:usesCleartextTraffic="true"`

Impact:
- Enables unencrypted HTTP traffic for all domains.
- If using a non-local backend over the internet, traffic can be intercepted/modified.

Recommendation:
- Prefer HTTPS endpoints and disable cleartext by default.
- If HTTP is required for local LAN, consider a Network Security Config that allows cleartext only for specific domains/IP ranges.

### F-05 (Medium): Sensitive data stored unencrypted and eligible for backup

Evidence:
- `app/src/main/AndroidManifest.xml#L32` sets `android:allowBackup="true"`
- Backup rules (`app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`) are essentially defaults (no meaningful exclusions).
- API key stored in DataStore (`PreferencesRepository`).
- Chat logs stored in Room (`SavedChatMessage`), and screenshots stored on disk (`BitmapUtils.saveBitmap`).

Impact:
- Backups/device transfers may copy API keys, prompts, and conversation history.
- Local malware with on-device access, rooted devices, or forensic extraction increases exposure.

Recommendation:
- Set `allowBackup=false` OR exclude sensitive data explicitly (settings, DB, screenshots).
- Encrypt sensitive fields:
  - API key: use Android Keystore + encrypted storage (e.g., `EncryptedSharedPreferences` or encrypted DataStore wrapper).
  - Consider encrypting chat DB and screenshots or add a “do not persist screenshots” option.

### F-06 (Medium): Privilege escalation / command execution paths (Shizuku / root)

Evidence:
- Root usage via `Runtime.getRuntime().exec("su")`:
  - `app/src/main/java/com/example/open_autoglm_android/util/AuthHelper.kt#L120` and `#L137+`
- Shizuku command execution uses reflection to call `newProcess`:
  - `AuthHelper.kt#L95-100`
- AIDL-backed `UserService` executes arbitrary commands:
  - `app/src/main/java/com/example/open_autoglm_android/UserService.kt#L16-26`
  - `app/src/main/aidl/com/example/open_autoglm_android/IUserService.aidl`

Impact:
- If a user grants Shizuku permission (or the device is rooted), the app can execute arbitrary shell commands.
- This is likely intended for granting `WRITE_SECURE_SETTINGS`, but it increases the impact of any app compromise.

Recommendation:
- Restrict command execution to a small allowlist (e.g., only `pm grant <self> WRITE_SECURE_SETTINGS`).
- Avoid exposing general-purpose “exec” methods unless strictly necessary.

### F-07 (Low/Informational): Accessibility service configured for broad capabilities

Evidence:
- `app/src/main/res/xml/accessibility_service_config.xml` sets:
  - `android:accessibilityEventTypes="typeAllMask"`
  - `android:canRetrieveWindowContent="true"`
  - `android:canTakeScreenshot="true"`
  - `android:canRequestFilterKeyEvents="true"`
  - `android:packageNames=""` (all apps)

Impact:
- High capability footprint increases trust requirements and “blast radius.”
- `canRequestFilterKeyEvents` appears unnecessary (no `onKeyEvent` implementation found), but it signals potential key-event access.

Recommendation:
- Minimize requested capabilities/flags where possible.
- Document clearly in README/privacy notice what is accessed and why.

## Backdoor/Tracker Checks (what we did NOT find)

Within `app/src/main/java`:
- No Firebase/Bugly/Umeng/Sentry analytics or crash upload libraries detected in Gradle or code.
- No additional hard-coded endpoints besides:
  - model backend base URL default
  - `https://shizuku.rikka.app/` link for installation guidance
- No dynamic code loading (`DexClassLoader`), embedded native libraries, or WebView JS bridges found.
- No device identifier harvesting (ANDROID_ID/IMEI/Advertising ID), contacts, location, or account access found.

## Recommendations (Prioritized)

1. **Remove/guard HTTP body logging** (`ModelClient`): disable in release; redact headers; do not log bodies containing base64 images.
2. **Stop logging typed text** in IME; consider masking or disabling IME mode by default.
3. **Do not default to a remote backend**; require user to explicitly set/confirm base URL before sending any screenshot.
4. **Disable backups or exclude sensitive stores**; encrypt API key and consider encrypting chat/screenshot artifacts.
5. **Reduce permissions**:
   - remove `MANAGE_EXTERNAL_STORAGE` if not strictly needed
   - consider removing legacy storage flags
   - reduce accessibility flags where possible (e.g., filter key events).
6. **Constrain command execution**:
   - allowlist commands for Shizuku/root usage; avoid generic shell execution interfaces.

## Suggested Next Steps

- If you want, I can implement the key mitigations (1–4) as a follow-up patch and add a “privacy disclosure + confirmation” screen before first task execution.

