# 部署与真机冒烟测试

本文档记录如何在本仓库中构建、安装、真机冒烟测试 **包子**（`com.lfr.baozi`），以及在命令行部署过程中遇到的问题与解决方案。

## 前置条件

- 已安装 Android SDK，`adb` 可用
- 有一台真机（或可用设备）通过 `adb devices` 能识别
- 仓库根目录：`Open-AutoGLM-Android/`

可选：
- 若要使用“高级授权与无感保活”（写 secure settings）能力，可通过 adb 授权：
  - `adb shell pm grant com.lfr.baozi android.permission.WRITE_SECURE_SETTINGS`

## 模型后端（OpenAI 兼容 / vLLM）

应用内置默认（可通过编译时环境变量注入）：
- `Base URL`：来自 `PHONE_AGENT_BASE_URL`（或 `BAOZI_DEFAULT_BASE_URL`）
- `API Key`：选填（留空不发送 `Authorization` 头）
- `最大步数`：默认 `20`

说明：
- 若要在“无需手动设置”的情况下使用自建后端，请在编译时设置环境变量：
  - `export PHONE_AGENT_BASE_URL="http://<你的服务端>/v1"`
- 为了支持 `http://`，manifest 中启用了 cleartext。

## 登录 / 账号

- 首次启动会进入登录页（邀请码校验逻辑后续接入）。
  - 邀请码可留空（默认使用 `123456`）。
  - 默认昵称为 **包子**，默认头像为 `avator.jpeg`。
- 登录后可在侧边栏底部点击“头像 + 昵称”进入个人资料页修改。

## 设置入口（改哪些）

- `设置` → `模型与执行`：最大步数 / 进入“自定义服务端”
- `设置` → `模型与执行` → `自定义服务端`：覆盖 Base URL / API Key（留空使用内置默认）

## 构建

在仓库根目录执行：

```bash
./gradlew :app:assembleDebug
```

产物：
- `app/build/outputs/apk/debug/app-debug.apk`

## 安装 / 重新安装（真机）

选择设备：

```bash
adb devices
export ANDROID_SERIAL=<device-serial>
```

安装/覆盖安装：

```bash
adb -s "$ANDROID_SERIAL" install -r -t app/build/outputs/apk/debug/app-debug.apk
```

可选：清空数据（全新启动）：

```bash
adb -s "$ANDROID_SERIAL" shell pm clear com.lfr.baozi
```

启动：

```bash
adb -s "$ANDROID_SERIAL" shell am start -n com.lfr.baozi/.MainActivity
```

## 真机冒烟测试

运行 instrumentation tests：

```bash
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

报告：
- HTML：`app/build/reports/androidTests/connected/debug/index.html`
- JUnit XML：`app/build/outputs/androidTest-results/connected/debug/`

覆盖点（示例）：
- vLLM `/v1/models` 在无 API Key 时可访问
- API Key 为空时，`ModelClient` 不发送 `Authorization`
- “模型与执行/自定义服务端”页默认不展示内置默认值

## 近期改动点（摘要）

### 后端默认 + API Key 可选
- 默认 Base URL 位于 `PreferencesRepository`
- API Key 为空时不附带鉴权头
- 设置页支持自定义覆盖 + “恢复默认”（清空覆盖项）

### 登录 + 个人资料
- 新增登录门禁（邀请码校验后续完善）
- 新增个人资料查看/编辑（头像、昵称可改；邀请码只读）

### 无障碍状态判断修复
问题：用户已开启无障碍，但 App 偶发仍提示未开启。

修复：
- 优先使用 `AccessibilityManager.getEnabledAccessibilityServiceList(...)`，再回退解析 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
- 设置页区分“未启用 / 已启用但连接中 / 已启用且运行中”
- 任务执行在服务未及时连接时会短暂等待

### 最大步数（Max Steps）
- 设置页增加最大步数（1~50）
- 通过 DataStore 持久化，默认 `20`
- 执行循环使用该配置而非写死 50

### 任务状态标识
- 每个任务记录状态：`未开始 / 进行中 / 完成 / 中止 / 结束`
- Room 增加字段并迁移
- 侧边栏列表显示对应状态

## 过程问题与解决

### Kotlin 编译 Daemon 不稳定（CLI）
- 现象：Gradle 编译过程中 Kotlin daemon 偶发崩溃
- 解决：改为 in-process：`gradle.properties` → `kotlin.compiler.execution.strategy=in-process`

### 代理配置导致依赖下载失败
- 现象：Gradle 解析依赖失败（空代理项）
- 解决：删除 `gradle.properties` 中空的代理项

### Instrumentation 因 Shizuku unbind 崩溃
- 现象：Activity 销毁时 Shizuku binder 不可用导致测试崩溃
- 解决：`Shizuku.unbindUserService(...)` 包裹 `try/catch IllegalStateException`

### UI 测试导航不稳定
- 现象：无法稳定用本地化文案定位组件
- 解决：为关键组件添加稳定的 Compose `testTag`
