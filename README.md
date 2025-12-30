# 包子（Open-AutoGLM-Android）

[English](README_EN.md) | 中文

一个基于 Android 无障碍服务的手机自动化助手：你用自然语言描述任务，应用会截图 → 调用 OpenAI 兼容模型 → 自动执行点击/滑动/输入等操作。

## 截图

<table>
  <tr>
    <td><img src="asserts/index.jpeg" width="240" alt="首页" /></td>
    <td><img src="asserts/menu.png" width="240" alt="侧边栏" /></td>
    <td><img src="asserts/settings.png" width="240" alt="设置" /></td>
  </tr>
  <tr>
    <td><img src="asserts/login.jpeg" width="240" alt="登录" /></td>
    <td><img src="asserts/custom.jpeg" width="240" alt="自定义服务端" /></td>
    <td><img src="asserts/sidecar.png" width="240" alt="历史记录" /></td>
  </tr>
</table>

## 功能

- 任务信息流：展示思考/动作/截图/状态
- 任务列表：历史任务、置顶、重命名、删除
- 可配置：最大步数（1~50，默认 20）、输入方式、模型后端
- 后端：OpenAI 兼容接口（vLLM 等），API Key 可选

## 新手教程（给用户）

1. 安装 APK 并打开应用
2. 登录页：邀请码可留空（默认使用 `123456`），昵称/头像可后续修改
3. 打开侧边栏 → 进入 `设置`
4. 在 `无障碍服务` 引导中开启服务（建议把系统电池策略设置为“无限制/不受限制”）
5. 回到首页，输入任务描述并发送，例如“打开浏览器，搜索天气”

## 新手教程（给开发者）

1. 可选：编译时注入默认后端（避免每次手动配置）
   - `export PHONE_AGENT_BASE_URL="http://<你的服务端>/v1"`
2. 本地构建：`./gradlew :app:assembleDebug`
3. 安装到设备：`./gradlew :app:installDebug`
4. 真机测试：`./gradlew :app:connectedDebugAndroidTest`
5. 部署/冒烟细节见：`docs/deploy.md`

## 设置说明

- `设置` → `模型与执行`
  - 最大步数：范围 1~50（建议 20~30）
  - 服务端：仅展示“自定义服务端”（不展示内置默认）
- `设置` → `模型与执行` → `自定义服务端`
  - `Base URL`：填写你的 OpenAI 兼容 `/v1` 地址（留空使用内置默认）
  - `API Key`：选填（留空不发送 `Authorization`）
  - 支持“恢复默认”（清空自定义覆盖）

## 构建与安装

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## 文档

- 部署/真机冒烟测试：`docs/deploy.md`
- 安全与隐私静态审查：`docs/SECURITY_AUDIT.md`

## 许可

MIT，见 `LICENSE`。
