# SaleSmartly 移动端接入与后台配置

## 当前版本与入口流程

| 平台 | SDK | 工程位置 |
| --- | --- | --- |
| Android | 官方 AAR v1.0.3 | `app/libs/salesmartly-chatwidget-sdk-v1.0.3.aar` |
| iOS | 官方原生 SDK v1.0.4 | `../ThrustAppIOS/Vendor/SalesmartlyChat/` |

两端都使用同一 SaleSmartly 项目脚本。用户点击 App 客服入口后，App 读取 SDK 返回的渠道配置并先展示选择页。点击 **Enter chat** 后，Android 调用 `openChat()`，iOS 调用 `openChatFromIconPopup()` 进入会话；点击 WhatsApp 则打开后台给出的链接。此流程不依赖 SDK 原生欢迎页的展示模式。

Android AAR v1.0.3 的 SHA-256：`43fb19660c81ce35aee746dab7b8bc5947f829b35464a637065c9ef5264e9ecb`。

## 后台配置

1. 确认 App 对应的项目脚本地址。当前默认值为 `https://plugin-code.salesmartly.com/js/project_830957_862319_1789665876.js`。
2. 在该项目聊天插件里启用 Chat、WhatsApp，并配置 WhatsApp 目标号码或跳转地址。发布后，SDK 返回的 `channel` 应包含 `chat` 和 `whatsapp`。
3. 需要 Telegram 时，可另行启用并配置；移动端会在返回的渠道列表中显示。
4. 建立 `android`、`ios`、`thrust_wealth` 标签；自定义字段需先在后台建立，再将字段 ID 写入移动端配置。
5. 当前项目返回的 `integration_type=column` 使 SDK 原生欢迎页未绘制渠道卡片。App 已依据 SDK 返回的 `channel` 和跳转配置自行显示选择页。若调整后台展示模式，仍需在移动端验收。

## Android 工程配置

本机 `local.properties` 可覆盖项目脚本：

```properties
salesmartly.scriptUrl=https://plugin-code.salesmartly.com/js/project_830957_862319_1789665876.js
```

首次使用前须完成隐私授权，之后调用 `SalesmartlyChat.initialize(context, scriptUrl)`。App 为匿名访客保存稳定 ID，聊天前通过 `setLoginInfo` 设置用户信息。清除数据时调用 `clearUser()`。

编译要求以工程当前 Gradle 配置为准，需 `compileSdk 36`。本地可用 `./gradlew :app:assembleDebug --offline` 验证。

## 验收

- [ ] 点击客服先看到 **Enter chat** 与后台启用的 WhatsApp，未直接进入聊天。
- [ ] 点击 **Enter chat** 才进入 SDK 会话；返回后可重新选择。
- [ ] 点击 WhatsApp 打开后台配置的号码和问候语。
- [ ] Android 与 iOS 使用相同项目、相同渠道配置。
- [ ] 客户资料中的匿名 ID 和标签正确；未读数按后台消息变化。

模拟器可验收页面和应用内聊天。WhatsApp App 的最终唤起须在安装 WhatsApp 的真机复核。

官方仓库：[SaleSmartly Mobile SDK](https://github.com/SaleSmartly-Mobile/Mobile-SDK)。
