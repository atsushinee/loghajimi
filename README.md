# loghajimi

![Build](https://github.com/atsushinee/loghajimi/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
### 增强您的 IntelliJ IDEA 日志体验

LogHajimi 是一款旨在将 Android Studio Logcat 般强大、流畅的日志查看体验带入 IntelliJ IDEA 的插件。它无缝集成到现有的 Run/Debug 控制台，为您提供一个功能更丰富的日志视图，让日志分析和问题排查变得前所未有的高效。

#### ✨ 核心功能

*   **一键切换增强视图**: 在 Run/Debug 控制台的工具栏上，LogHajimi 会添加一个专属的🔍图标。点击它，即可在一个新的、独立的 **LogHajimi** 标签页中打开增强日志视图，原始控制台不受任何影响。

*   **强大的实时过滤**:
    *   **多关键词 (OR)**: 在顶部的 "Filter" 输入框中，您可以输入由**空格**分隔的多个关键词（例如 `error warning`）。插件会实时显示所有包含**任意一个**关键词的日志行。
    *   **不区分大小写**: 所有过滤操作均不区分大小写，让您专注于问题本身。

*   **智能滚动与自动换行**:
    *   **智能滚动**: 新日志的自动滚动功能变得更加智能。只有当您已经位于日志末尾时，视图才会自动向下滚动。一旦您向上滚动查看历史记录，自动滚动就会停止，不会打断您的操作。
    *   **自动换行**: 长日志行会自动换行，确保所有信息都能在不横向滚动的情况下完整显示。

*   **独立的生命周期管理**:
    *   **独立标签页**: LogHajimi 视图在专属的标签页中打开，您可以随时在它和原始控制台之间自由切换。
    *   **自动关闭**: 当原始的 Run/Debug 会话结束并关闭其标签页时，与之关联的 LogHajimi 标签页也会被**自动关闭**，有效防止了状态混乱和内存泄漏。

*   **一键清空**: 过滤框旁边提供了一个方便的清除按钮，让您可以随时清空当前视图的所有内容，重新开始。

LogHajimi 致力于成为您在 IntelliJ IDEA 中进行日志分析的得力助手。
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "loghajimi"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/atsushinee/loghajimi/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
