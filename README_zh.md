![Now in Android](docs/images/nia-splash.jpg "Now in Android")

<a href="https://play.google.com/store/apps/details?id=com.google.samples.apps.nowinandroid"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="70"></a>

Now in Android 应用
==================

**了解此应用如何在 [设计案例研究](https://goo.gle/nia-figma)、[架构学习之旅](docs/ArchitectureLearningJourney.md) 和 [模块化学习之旅](docs/ModularizationLearningJourney.md) 中被设计和构建。**

这是 [Now in Android](https://developer.android.com/series/now-in-android) 应用的代码仓库。该项目处于 **开发中** 🚧。

**Now in Android** 是一个完整功能的 Android 应用，完全使用 Kotlin 和 Jetpack Compose 构建。它遵循 Android 的设计与开发最佳实践，旨在为开发者提供一个参考示例。作为一个运行中的应用，它旨在通过定期的新闻更新帮助开发者了解 Android 开发领域的最新动态。

该应用仍在开发中。`prodRelease` 变体可在 [Play 商店](https://play.google.com/store/apps/details?id=com.google.samples.apps.nowinandroid) 获得。

# 功能

**Now in Android** 展示来自 [Now in Android](https://developer.android.com/series/now-in-android) 系列的内容。用户可以浏览指向最近视频、文章和其他内容的链接。用户还可以关注自己感兴趣的主题，并在有匹配的新内容发布时收到通知。

## 截图

![展示“为你推荐”界面、兴趣界面和主题详情界面的截图](docs/images/screenshots.png "Screenshot showing For You screen, Interests screen and Topic detail screen")

# 开发环境

**Now in Android** 使用 Gradle 构建系统，可以直接导入到 Android Studio（请确保使用最新稳定版，下载地址：[Android Studio](https://developer.android.com/studio)）。

将运行配置改为 `app`。

![image](https://user-images.githubusercontent.com/873212/210559920-ef4a40c5-c8e0-478b-bb00-4879a8cf184a.png)

`demoDebug` 和 `demoRelease` 构建变体可以被构建和运行（`prod` 变体使用一个当前不可公开访问的后端服务器）。

![image](https://user-images.githubusercontent.com/873212/210560507-44045dc5-b6d5-41ca-9746-f0f7acf22f8e.png)

一旦环境搭建完毕，你可以参考下面的学习之旅来更好地理解项目中使用的库和工具、在 UI、测试、架构等方面的设计理由，以及这些不同模块如何协同构成一个完整的应用。

# 架构

**Now in Android** 遵循 [官方架构指南](https://developer.android.com/topic/architecture)，并在[架构学习之旅](docs/ArchitectureLearningJourney.md)中有详细说明。

# 模块化

**Now in Android** 已经实现了完整的模块化。你可以在[模块化学习之旅](docs/ModularizationLearningJourney.md)中找到关于模块化策略的详细指导和说明。

# 构建

应用包含常见的 `debug` 与 `release` 构建变体。

此外，`app` 模块的 `benchmark` 变体用于测试启动性能并生成基线配置（baseline profile，详见下文）。

`app-nia-catalog` 是一个独立应用，用于显示为 **Now in Android** 风格化的组件列表。

该应用还使用了[产品风味（product flavors）](https://developer.android.com/studio/build/build-variants#product-flavors)来控制应用加载内容的来源。

`demo` flavor 使用静态本地数据，便于立即构建并探索 UI。

`prod` flavor 会对后端服务器发起真实网络请求以获取最新内容。目前没有公开的后端可用。

日常开发请使用 `demoDebug` 变体。进行 UI 性能测试时请使用 `demoRelease` 变体。

# 测试

为了方便对组件进行测试，**Now in Android** 使用 [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) 进行依赖注入。

大多数数据层组件以接口形式定义。然后绑定具体实现（以及它们的依赖）以向应用中的其他组件提供这些接口。在测试中，**Now in Android** 倾向于不使用任何 mocking 库。相反，可以使用 Hilt 的测试 API（或对 `ViewModel` 测试使用手动构造器注入）替换生产实现为测试用的替代实现（test doubles）。

这些测试替代实现实现与生产实现相同的接口，通常提供简化但仍然比较真实的实现，并包含额外的测试钩子。这能带来更不易脆弱的测试，可能会驱动更多的生产代码执行，而不是仅仅验证对 mock 的特定调用。

示例：
- 在 instrumentation 测试中，会使用临时文件夹来存储用户偏好（preferences），该文件夹会在每个测试结束后清理。这样可以使用真实的 `DataStore` 并执行所有相关代码，而不是模拟数据流的更新。
- 每个仓库（repository）都有对应的 `Test` 实现，该实现既实现了完整的仓库接口，也提供测试专用的钩子。`ViewModel` 测试使用这些 `Test` 仓库，因此可以使用测试钩子来操纵 `Test` 仓库的状态并验证相应行为，而不是检查是否调用了某个具体的仓库方法。

要运行测试，请执行下列 gradle 任务：

- `testDemoDebug`：针对 `demoDebug` 变体运行所有本地测试。截图测试会失败（见下文解释）。为避免这种情况，请在运行单元测试前先运行 `recordRoborazziDemoDebug`。
- `connectedDemoDebugAndroidTest`：针对 `demoDebug` 变体运行所有仪器化测试（需连接设备或模拟器）。

> [!NOTE]
> 不要运行 `./gradlew test` 或 `./gradlew connectedAndroidTest`，因为这将对所有构建变体执行测试，这既不必要也会导致失败——当前只有 `demoDebug` 变体受支持。其他变体没有测试（未来可能会改变）。

## 截图测试

截图测试会对屏幕或 UI 组件进行截屏，并将其与仓库中事先录制的已知正确截图进行比较。

例如，Now in Android 在 [此处示例](https://github.com/android/nowinandroid/blob/main/app/src/testDemo/kotlin/com/google/samples/apps/nowinandroid/ui/NiaAppScreenSizesScreenshotTests.kt) 有截图测试，用以验证导航在不同屏幕尺寸上的显示是否正确（[已知正确的截图存放位置](https://github.com/android/nowinandroid/tree/main/app/src/testDemo/screenshots)）。

Now In Android 使用 [Roborazzi](https://github.com/takahirom/roborazzi) 来运行某些屏幕和 UI 组件的截图测试。与截图测试相关的常用 gradle 任务如下：

- `verifyRoborazziDemoDebug`：运行所有截图测试，将当前截图与已知正确截图进行校验。
- `recordRoborazziDemoDebug`：录制新的“已知正确”截图。当你更改了 UI 并人工确认渲染正确时使用此命令。截图将被存储在 `modulename/src/test/screenshots`。
- `compareRoborazziDemoDebug`：针对失败的测试生成比较图片（failed vs known good），这些图片也会保存在 `modulename/src/test/screenshots`。

> [!NOTE]
> **关于截图测试失败的说明**
> 仓库中存储的已知正确截图是在 CI（Linux）上录制的。其他平台可能（并且通常会）生成略有不同的图像，导致截图测试失败。当在非 Linux 平台工作时，一种变通办法是在开始工作前于 `main` 分支上运行 `recordRoborazziDemoDebug`。在你进行代码修改后，运行 `verifyRoborazziDemoDebug` 将只会识别真实的变更。

更多关于截图测试的信息可以参见 [这次演讲](https://www.droidcon.com/2023/11/15/easy-screenshot-testing-with-compose/)。

# UI

该应用遵循 [Material 3 规范](https://m3.material.io/)。了解更多设计流程并获取设计文件，请参阅 [Now in Android Material 3 案例研究](https://goo.gle/nia-figma)（设计资源也可通过 PDF 获取：`docs/Now-In-Android-Design-File.pdf`）。

屏幕与 UI 元素完全使用 [Jetpack Compose](https://developer.android.com/jetpack/compose) 构建。

应用有两套主题：

- 动态配色（Dynamic color）——根据用户当前的系统配色主题使用颜色（如受支持）。
- 默认主题（Default theme）——在系统不支持动态配色时使用预定义颜色。

每个主题也都支持暗色模式（Dark mode）。

该应用使用自适应布局来[支持不同屏幕尺寸](https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes)。

更多关于 UI 架构的信息见 [架构学习之旅 - UI 层](docs/ArchitectureLearningJourney.md#ui-layer)。

# 性能

## 基准测试（Benchmarks）

所有使用 [`Macrobenchmark`](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) 编写的测试位于 `benchmarks` 模块。该模块还包含用于生成基线配置（baseline profile）的测试。

## 基线配置（Baseline profiles）

该应用的基线配置位于 [`app/src/main/baseline-prof.txt`](app/src/main/baseline-prof.txt)。它包含能在应用启动关键路径上启用 AOT 编译的规则。
有关基线配置的更多信息，请阅读 [相关文档](https://developer.android.com/studio/profile/baselineprofiles)。

> [!NOTE]
> 基线配置需要在影响应用启动的发布构建中重新生成（如果代码有变动）。

要生成基线配置，请选择 `benchmark` 构建变体，并在 AOSP Android 模拟器上运行 `BaselineProfileGenerator` 基准测试。然后将生成的基线配置从模拟器拷贝到 [`app/src/main/baseline-prof.txt`](app/src/main/baseline-prof.txt)。

## Compose 编译器指标（Compose compiler metrics）

运行下列命令以获取并分析 Compose 编译器的指标：

```bash
./gradlew assembleRelease -PenableComposeCompilerMetrics=true -PenableComposeCompilerReports=true
```

报告文件将被写入到 [build/compose-reports](build/compose-reports)。指标文件也会被写入到 [build/compose-metrics](build/compose-metrics)。

有关 Compose 编译器指标的更多信息，请参阅 [这篇博客](https://medium.com/androiddevelopers/jetpack-compose-stability-explained-79c10db270c8)。

# 许可证

**Now in Android** 根据 Apache License（第 2.0 版）发布。更多信息请参阅 [LICENSE](LICENSE)。

