# SaleSmartly 接入配置指南（后台 + 移动端）

面向：后台/运营、Android 开发、iOS 开发。

官方文档入口：https://help.salesmartly.com/docs/product-introduction  
Android 原生 SDK：https://github.com/SaleSmartly-Mobile/Mobile-SDK

---

## 1. 方案确认

| 平台 | 接入方式 | 关键配置 |
|------|----------|----------|
| Android | **原生 SDK**（非 WebView） | `project scriptUrl` + AAR |
| iOS | H5 / WebView（按当前规划） | 专属链接 / H5 页面地址 |

Android 使用新版 API：`SalesmartlyChat.initialize` / `setLoginInfo`，**不要**再使用旧版 `SaleSmartyInit.uploadUserMessage`。

---

## 2. 后台在 SaleSmartly 控制台需完成的事项

| 步骤 | 操作 | 交付给移动端 |
|------|------|----------------|
| 1 | 登录控制台 → 进入对应项目 | — |
| 2 | 复制 **项目 JS 脚本地址**（形如 `https://assets.salesmartly.com/js/project_xxxxx.js`） | `salesmartly.scriptUrl` |
| 3 | 创建访客标签（须与 App 传参完全一致） | 标签名列表 |
| 4 | 「项目设置 → 自定义字段」创建字段，记录每个字段的 **ID** 与类型 | `custom_fields_ext` 映射表 |
| 5 | 配置聊天窗口外观、欢迎语、自动回复 | — |
| 6 | （可选）生成 **专属链接 / H5** | 仅 iOS WebView 需要 |

建议预置标签示例：`android`、`ios`、`thrust_wealth`、`free_user`。

当前 Android 默认会传：`["android", "thrust_wealth"]`（须已在后台创建，否则无效）。

---

## 3. 交付给移动端的信息清单（核对表）

```text
1. project scriptUrl
   示例：https://assets.salesmartly.com/js/project_xxxxx.js
   Android：SalesmartlyChat.initialize(context, scriptUrl)
   iOS WebView：不需要（走专属链接）

2. Android SDK AAR
   文件：salesmartly-chatwidget-sdk-v1.0.0.aar
   仓库：https://github.com/SaleSmartly-Mobile/Mobile-SDK

3. 标签名列表 label_names
   示例：["android", "thrust_wealth", "free_user"]
   注意：只能传控制台已创建的标签；覆盖式更新

4. 自定义字段 ID 及类型 custom_fields_ext
   示例：{"1210":"pro","plan":["monthly"]}
   选择类型字段传数组；在「客户信息」中可核验

5. 用户身份字段规范
   - user_id：必填，加密/稳定访客 ID，最长 300 字符
   - user_name：必填，展示名
   - language：如 en-US
   - phone：含国际区号，如 +8613800138000
   - email / description：可选

6. 专属链接 / H5（仅 iOS）
   示例：https://chat.example.com/service/xxxxx
   Android 原生 SDK 不需要
```

---

## 4. Android 工程侧配置（开发自检）

1. 将 AAR 放在 `app/libs/`（已纳入版本库例外规则 `!app/libs/*.aar`）。
2. 工程已写入当前项目地址；如需切换 SaleSmartly 项目，可在 **本机** `local.properties` 覆盖：

```properties
salesmartly.scriptUrl=https://plugin-code.salesmartly.com/js/project_830957_862319_1789665876.js
```

3. 重新 Sync / Rebuild，使 `BuildConfig.SALESMARTLY_SCRIPT_URL` 生效。
4. 首次启动须同意隐私弹窗后，才会调用 `SalesmartlyManager.initializeIfNeeded()`。
5. Settings → **Customer Support** 进入聊天页。

### 工程版本要求（与 AAR v1.0.0 对齐）

| 项 | 最低要求 | 说明 |
|----|----------|------|
| compileSdk | 36 | AAR 强制要求 |
| targetSdk | 35（可保持） | 与 compileSdk 可解耦 |
| Kotlin | 2.3.x | AAR 含 Kotlin metadata 2.3.0 |
| KSP | 2.3.4+ | 与 Kotlin 2.3 配套 |
| Room | 2.8.x | 避免 KSP 2.3 下签名解析错误 |
| Hilt | 2.58+ | 支持 Kotlin 2.3 metadata |

### 身份传递（App 无账号体系时）

- 本地生成并持久化 `guestUserId`（`tw_` 前缀）。
- 同意隐私后调用：

```kotlin
SalesmartlyChat.setLoginInfo(
    LoginInfo(
        user_id = guestUserId,
        user_name = "Thrust Guest",
        language = "en-US",
        label_names = listOf("android", "thrust_wealth"),
        custom_fields_ext = emptyMap(), // 后台提供字段 ID 后再填
    )
)
```

退出或重置访客身份时调用 `SalesmartlyChat.clearUser()`。

---

## 5. 合规要求（上架前）

- **首次冷启动**：用户同意隐私政策之后，才允许 `initialize`。
- App 隐私政策须披露 SaleSmartly SDK（名称、公司、目的、字段、权限、隐私链接）。
- 权限：`INTERNET` 必需；`POST_NOTIFICATIONS`、媒体读取为可选。

隐私披露模板见官方：  
https://help.salesmartly.com/docs/SaleSmartly-yuan-sheng-SDK-he-gui-pei-zhi-zhi-yin

---

## 6. 修正后的跨端对照表

| 信息项 | Android | iOS（H5） |
|--------|---------|-----------|
| project scriptUrl | `initialize` 必需 | 不需要 |
| 专属链接 / H5 | 不需要 | WebView 加载必需 |
| SDK 产物 | AAR v1.0.0 | — / SwiftPM（若改原生） |
| 标签名 | `setLoginInfo(label_names)` | `ssq.push('setLoginInfo', …)` |
| 自定义字段 | `custom_fields_ext` | 同左 |
| 用户身份 | `setLoginInfo(user_id, user_name, …)` | `setLoginInfo` 或 postMessage |

---

*文档版本：与 Thrust Wealth Android SaleSmartly 接入同步。*
