# 安全与隐私审查报告（静态审查）

日期：2025-12-30  
项目：`Open-AutoGLM-Android`（包子 / `com.lfr.baozi`）  
审查版本：`00c87c13ae9dd52a149bdb195ca45ea0c1d41c15`  
方法：人工静态审查（源代码 + Manifest + 构建配置），未进行抓包/动态流量验证。

## 结论摘要

该项目是一个基于 **AccessibilityService + 截图** 的设备端自动化 Agent，并通过 **OpenAI 兼容 HTTP 后端** 进行推理决策。由于会采集屏幕截图并可能包含其他应用 UI 内容，因此**隐私风险天然较高**（这是功能设计所决定的）。

主要结论：

- 未发现“隐藏的后门域名/暗链上报”特征：网络访问主要集中在模型后端 Base URL（以及 Shizuku 官网引导链接）。
- 存在高隐私风险点（多数属于实现方式导致的风险放大），尤其是：
  - OkHttp 开启了 `HttpLoggingInterceptor.Level.BODY`（可能把截图 base64、提示词、响应内容以及敏感头写入 logcat）。
  - 输入法服务会把实际输入的文本打印到 logcat（可能泄露密码/验证码/隐私内容）。
  - API Key、聊天记录、截图路径等以明文方式存储，并且应用允许备份（`allowBackup=true`）。
  - 默认后端 Base URL 为**编译时注入**（`BuildConfig.DEFAULT_BASE_URL`），若配置为公网地址，任务执行时可能把截图/指令发送到该服务器。

## 审查范围

已审查：
- `AndroidManifest.xml`：权限、组件导出、网络策略（cleartext）等
- 网络层：Retrofit/OkHttp、鉴权头、请求体内容（是否包含截图/文本）
- 存储：DataStore/Room/外部文件（API Key、会话记录、截图）
- 特权路径：Shizuku/Root 命令执行
- Gradle 依赖（是否包含常见统计/上报 SDK）

未审查：
- 远端模型后端本身的日志/留存/安全策略
- Maven/Gradle 供应链风险（超出声明依赖的审计）
- 动态流量行为（未抓包验证）

## 威胁模型（本报告中“后门/窃取/上报”的含义）

重点检查：
- 是否存在“用户无感知”的额外网络上报端点
- 是否采集密码/API Key/隐私信息并发送到非预期服务
- 是否存在动态加载/远程执行（DexClassLoader 等）
- 是否存在剪贴板/联系人/定位等隐私收集（非功能必需）
- Shizuku/Root 是否被用于执行非必要的任意命令

## 应用能力与攻击面

### 权限（高层）
来源：`app/src/main/AndroidManifest.xml`

- 网络：`INTERNET`、`ACCESS_NETWORK_STATE`
- 广泛应用可见性：`QUERY_ALL_PACKAGES`
- 存储（较宽）：`READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`、`MANAGE_EXTERNAL_STORAGE`
- 悬浮窗：`SYSTEM_ALERT_WINDOW`
- 开机启动：`RECEIVE_BOOT_COMPLETED`
- 前台服务：`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`
- 特权写入 secure settings：`WRITE_SECURE_SETTINGS`（通常需要 adb 授权、系统签名或 Shizuku/root）

### 导出组件
来源：`app/src/main/AndroidManifest.xml`

- `MainActivity`：作为 launcher 导出
- 无障碍服务：导出但受 `BIND_ACCESSIBILITY_SERVICE` 约束（系统绑定）
- 输入法服务：导出但受 `BIND_INPUT_METHOD` 约束（系统绑定）
- Boot receiver：导出并响应开机广播
- Quick Settings tile：导出并受 `BIND_QUICK_SETTINGS_TILE` 约束
- Shizuku provider：由 Shizuku 库引入

## 数据流（哪些数据会离开设备）

### 模型后端请求（预期行为，但高度敏感）
来源：`app/src/main/java/com/lfr/baozi/network/ModelClient.kt`

- 请求：`POST {baseUrl}/chat/completions`
- 典型请求体包含：
  - 用户任务描述（文本）
  - 当前应用包名等上下文（文本）
  - **屏幕截图**（通常为 `data:image/jpeg;base64,...` 的形式）
- API Key 非空时会带 `Authorization: Bearer <key>`；为空时不发送鉴权头。

含义：只要任务执行过程中采集截图，任何屏幕可见信息（包括聊天内容、支付信息、验证码、相册等）都有可能被发送到配置的模型后端。

### 本地持久化（敏感）

- 设置项（Base URL / API Key / max steps 等）存储在 DataStore（明文）：
  - `app/src/main/java/com/lfr/baozi/data/PreferencesRepository.kt`
- 会话/消息记录存储在 Room（明文）：
  - `app/src/main/java/com/lfr/baozi/data/database/SavedChatMessage.kt`
- 执行动作截图（包含标注）写入外部应用目录：
  - `app/src/main/java/com/lfr/baozi/util/BitmapUtils.kt`
  - 路径：`context.getExternalFilesDir("action_images")/...`

## 发现项（Findings）

严重级别：Critical / High / Medium / Low / Info。

### F-01（Critical）：OkHttp BODY 级别日志可能泄露截图/提示词/API Key

证据：
- `app/src/main/java/com/lfr/baozi/network/ModelClient.kt` 使用 `HttpLoggingInterceptor.Level.BODY`。
- 请求体包含 base64 截图与文本；若启用 API Key，鉴权头也可能进入日志（取决于拦截器与设备日志采集）。

影响：
- `logcat` 中可能出现敏感截图内容、提示词、模型输出、甚至鉴权信息。
- 设备日志可能被调试工具、OEM 采集、第三方日志工具或崩溃上报间接获取。

建议：
- Release 构建应禁用 BODY 日志（甚至禁用网络日志）。
- 至少在日志拦截器中 `redactHeader("Authorization")`，并避免打印请求体（尤其是图像 base64）。

### F-02（High）：输入法服务记录真实输入文本（可能泄露密码/验证码）

证据：
- `app/src/main/java/com/lfr/baozi/service/MyInputMethodService.kt` 存在“正在通过 IME 输入文本：$text”的日志。

影响：
- 若输入内容为密码/验证码/隐私数据，会被写入日志。

建议：
- 移除该日志或对内容脱敏（例如只记录长度/类型，不记录原文）。
- 对密码框/敏感输入场景做检测与保护（避免输入/避免记录/提示用户）。

### F-03（High）：默认后端指向远端地址（默认即可能外发数据）

证据：
- 默认 Base URL 通过编译时注入（`BuildConfig.DEFAULT_BASE_URL`），并会在用户未设置自定义服务端时生效。

影响：
- 用户若未修改设置，执行任务时可能直接把截图/提示词发到默认后端（取决于构建时注入的地址）。

建议：
- 更安全的默认：Base URL 为空或指向本地占位，首次发送前弹出“隐私提示 + 明确确认”。
- 明确告知“会发送截图/任务描述/当前应用信息到指定后端”。

### F-04（High）：允许 cleartext HTTP（全局）

证据：
- `AndroidManifest.xml` 中 `android:usesCleartextTraffic="true"`。

影响：
- 若使用公网 HTTP 后端，流量可能被窃听/篡改。

建议：
- 优先使用 HTTPS。
- 如需支持局域网 HTTP，使用 Network Security Config 将 cleartext 限制在特定域名/IP。

### F-05（Medium）：敏感数据明文存储且允许备份

证据：
- `AndroidManifest.xml`：`android:allowBackup="true"`
- API Key/设置、聊天记录、截图路径均可被备份/迁移或在本机被取证。

影响：
- 设备迁移/备份可能带出 API Key 与历史任务内容。
- Root/取证/恶意软件等场景下风险更高。

建议：
- 关闭备份（`allowBackup=false`）或对敏感数据做备份排除。
- API Key 用 Keystore + 加密存储（例如加密的 SharedPreferences/DataStore 包装）。

### F-06（Medium）：Shizuku/Root 可执行 shell 命令（权限放大）

证据：
- Root 通过 `su` 执行命令：`app/src/main/java/com/lfr/baozi/util/AuthHelper.kt`
- Shizuku 通过 UserService 执行命令：
  - `app/src/main/java/com/lfr/baozi/UserService.kt`
  - `app/src/main/aidl/com/lfr/baozi/IUserService.aidl`

影响：
- 一旦用户授予 Shizuku 权限或设备已 root，应用具备执行任意 shell 命令的能力（风险放大）。

建议：
- 将命令执行收敛为白名单（只允许必要命令，例如授权自身 `WRITE_SECURE_SETTINGS`）。
- 避免暴露通用 exec 接口。

### F-07（Info）：无障碍配置能力面较广（功能需要，但需明确告知）

证据：
- 无障碍服务配置可能请求较广泛事件/窗口内容/截图等能力。

影响：
- 能力越强，对用户信任要求越高；也需要更明确的隐私声明。

建议：
- 尽可能最小化不必要能力（例如若未使用 key event 过滤，可考虑去除）。
- 在 README/应用内说明中明确“采集哪些数据、何时采集、发往哪里、如何关闭/清理”。

## “未发现”的内容（有助于排除后门/跟踪）

在 `app/src/main/java` 与 Gradle 依赖层面，未发现：
- Firebase/Umeng/Bugly/Sentry 等常见统计/崩溃上报 SDK（以代码与依赖静态检查为准）
- 除模型后端外的额外硬编码上报域名/IP（除 Shizuku 官网引导链接）
- 动态代码加载（`DexClassLoader`）、WebView JS Bridge、可疑 native so
- 设备标识符/联系人/定位/账号等与功能无关的收集逻辑

## 建议的下一步（按优先级）

1. 关闭或严格限制网络 BODY 日志；对 `Authorization` 做 redact。
2. 移除输入法中对输入明文的日志。
3. 默认不指向远端后端；首次发送前做隐私确认。
4. 关闭备份或排除敏感数据；对 API Key 加密存储。
5. 收敛 Shizuku/Root 命令执行为白名单。
6. 进一步最小化权限与无障碍能力（在不影响功能的前提下）。
