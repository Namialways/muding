# 幕钉

一个面向 Android 的悬浮截图与贴图工具。

核心目标是让截图、贴图、OCR 和后续轻量编辑尽量在一条短链路里完成，而不是在相册、编辑器和分享面板之间来回跳转。

## 当前能力

- 悬浮球截图
- 截图后直接贴图，或进入编辑器
- 相册贴图
- 图片 OCR
- 剪贴板文字贴图
- 贴图历史与恢复
- 悬浮球外观自定义
- 简单标注编辑与编辑后贴图

## 技术栈

- Kotlin
- Jetpack Compose
- Android SDK 34
- Min SDK 26
- Java 17
- Gradle 8.7
- ML Kit OCR / Translate

## 运行前提

本项目依赖以下 Android 能力：

- 悬浮窗权限
- MediaProjection 截图授权
- 前台服务通知权限
- 相册读写相关权限（不同 Android 版本行为不同）

首次体验不完整通常不是代码没跑起来，而是权限还没给全。

## 本地构建

```powershell
./gradlew.bat assembleDebug
```

运行单元测试：

```powershell
./gradlew.bat testDebugUnitTest
```

## 项目状态

这个仓库目前还处在持续迭代阶段，重点在：

- 截图链路流畅度
- 贴图交互体验
- 悬浮球外观与默认配置
- OCR / 翻译配置整理

如果你准备继续完善它，优先建议先做真机体验验证，再做功能扩张。

## 说明

- 本仓库默认忽略本地构建缓存、IDE 配置和过程型 `docs/superpowers` 文档。
- `local.properties`、构建产物和本地缓存不应提交。
