# Now in Android — 关键流程与类概览（中文）

说明：本文档对仓库中 `app` 模块及相关核心模块的启动、导航、数据与依赖注入（DI）流程做简洁说明，列出常用的关键类/组件并说明它们在大流程中的位置与作用，方便快速理解代码走向与修改点。

## 快速总览（高层次序列）
1. 应用启动：`NiaApplication.onCreate()`
2. Activity 启动：`MainActivity.onCreate()` → 安装 Splash、收集主题设置
3. UI 状态构建：`MainActivityViewModel.uiState` 与 系统暗色流 组合 → 计算 `ThemeSettings`
4. Compose 树创建：`setContent` → `rememberNiaAppState(...)` → `NiaTheme` → `NiaApp`
5. 应用顶层渲染：`NiaApp` 显示背景/渐变、管理 Snackbar、显示 `SettingsDialog`、渲染导航套件
6. 导航与页面：`NiaNavigationSuiteScaffold` 使用 `TOP_LEVEL_NAV_ITEMS` + `NavDisplay` 与 `entryProvider` 来挂载不同 feature 的 entry（forYou/bookmarks/interests/topic/search）
7. 数据流与未读提示：各 feature 使用 `core` 模块中的仓库（如 `UserNewsResourceRepository`）与 `NetworkMonitor`/`TimeZoneMonitor` 提供数据，`NiaAppState` 将这些流组合并暴露给 UI
8. 性能与测试：`benchmarks` 模块（Macrobenchmark）与基线 profile、`Roborazzi` 截图测试在 CI/本地用于验证性能与 UI

---

## 启动流程（关键类/函数）
- `NiaApplication`（app/NiaApplication.kt）
  - 应用入口；初始化全局服务（例如 `Sync.initialize()`）。
  - 提供 Coil `ImageLoader`（通过 `ImageLoaderFactory` / dagger lazy 注入）。
  - 启动时触发 `ProfileVerifierLogger` 用于记录/验证基线 profile 是否已被安装/编译。

- `MainActivity`（MainActivity.kt）
  - 安装 Android Splash Screen；创建并收集主题设置（合并系统暗色/用户偏好）；调用 `enableEdgeToEdge()` 控制系统栏样式。
  - 保持启动屏直到 `MainActivityViewModel.uiState` 表示可以展示 UI。
  - `setContent { ... }` 内部创建 `rememberNiaAppState(...)` 并注入到下层 Compose。
  - 管理 `JankStats` 的启停（`onResume/onPause`）。

- `MainActivityViewModel`（MainActivityViewModel.kt）
  - 负责读取 `UserDataRepository.userData` 并将其转成 `StateFlow<MainActivityUiState>`，供 Activity 决定主题/动态配色、是否保留 splash 等。

---

## 应用级状态与 UI 入口
- `rememberNiaAppState(...)`（NiaAppState.kt）
  - 创建 `NiaAppState` 实例并记住（remember）。
  - 初始化内部 `NavigationState`（默认 `ForYouNavKey`）并触发导航性能追踪（`NavigationTrackingSideEffect`）。

- `NiaAppState`（NiaAppState.kt）
  - 暴露对外使用的 StateFlow：
    - `isOffline`（基于 `NetworkMonitor`）
    - `topLevelNavKeysWithUnreadResources`（基于 `UserNewsResourceRepository` 的关注和书签未读组合）
    - `currentTimeZone`（基于 `TimeZoneMonitor`）
  - 提供 `navigationState` 给 `Navigator` 与 Compose 层使用。

- `NiaApp`（ui/NiaApp.kt）
  - 顶层 Composable：渲染 `NiaBackground` / `NiaGradientBackground`（在 ForYou 页面使用渐变背景），管理 `SnackbarHost`（离线提示）、设置对话框、以及主导航框架 `NiaNavigationSuiteScaffold`。
  - 将 `LocalSnackbarHostState` 提供给需要的 feature（例如书签模块）。

---

## 导航体系与页面挂载（关键类）
- `TOP_LEVEL_NAV_ITEMS`（navigation/TopLevelNavItem.kt）
  - 定义顶层导航的 metadata（图标、标签、标题资源），并将 `NavKey` 映射到 `TopLevelNavItem`。

- `NavigationState` / `rememberNavigationState`（core/navigation）
  - 代表当前导航栈/当前 key、以及顶层导航键集合。

- `Navigator`（core/navigation/Navigator.kt）
  - 封装导航动作（navigate/goBack 等），与 `NavigationState` 协作。

- `entryProvider` 与 `NavDisplay`（使用 navigation3 runtime/ui）
  - `entryProvider { forYouEntry(navigator); bookmarksEntry(navigator); ... }` 把 feature 的 entry 注册到导航系统。
  - `NavDisplay(entries = navigationState.toEntries(entryProvider), ...)` 根据屏幕与 `sceneStrategy`（如 list-detail）渲染合适的页面布局。

- feature 的 entry
  - 每个 feature 模块（`feature/foryou`、`feature/bookmarks`、`feature/interests`、`feature/search`、`feature/topic`）都会暴露一个 `xxxEntry(navigator)`，该 entry 负责提供可导航的屏幕/节点并连接自己的 ViewModel 与仓库。

流程示意（简化）：
MainActivity -> rememberNiaAppState -> NiaApp -> NiaNavigationSuiteScaffold -> TOP_LEVEL_NAV_ITEMS -> NavDisplay -> entryProvider -> featureEntry -> feature UI

---

## 数据层与仓库（关键类）
- `UserNewsResourceRepository`（core/data）
  - 暴露 `observeAllForFollowedTopics()` 与 `observeAllBookmarked()` 等接口，用于计算 "未读" 状态。

- `UserDataRepository`（core/data）
  - 提供当前用户配置（如是否使用动态配色，主题偏好），被 `MainActivityViewModel` 订阅。

- `NetworkMonitor` / `TimeZoneMonitor`（core/data/util）
  - `NetworkMonitor.isOnline` 用于生成 `NiaAppState.isOffline`。
  - `TimeZoneMonitor.currentTimeZone` 提供当前时区 `StateFlow` 给 UI 本地化显示。

数据流示例：
- 未读小红点：UserNewsResourceRepository.observeAllForFollowedTopics() + observeAllBookmarked() -> NiaAppState.topLevelNavKeysWithUnreadResources -> NiaApp 渲染小红点
- 离线提示：NetworkMonitor.isOnline -> NiaAppState.isOffline -> NiaApp 的 SnackbarHost 弹窗

---

## 依赖注入（DI）与应用扩展
- Hilt（dagger.hilt）用于注入 Application/Activity/Feature 级别的依赖。
- 重要模块：
  - `JankStatsModule`（app/di）提供 `JankStats`、`OnFrameListener` 等（用于卡顿监控）。
  - 其他 feature-specific 的 Hilt modules 分散在各模块内（core/feature 等）。

- `NiaApplication`：Hilt 应用入口；提供 ImageLoader、在启动时触发 Sync 与 ProfileVerifierLogger。

---

## 性能、基线与测试
- `benchmarks` 模块包含 Macrobenchmark 测试，用于生成基线 profile（baseline-prof.txt）。
- `ProfileVerifierLogger`：用于检查并记录基线 profile 的安装/编译状态，便于在本地验证优化是否生效。
- UI 截图测试使用 `Roborazzi`：任务示例 `recordRoborazziDemoDebug`、`verifyRoborazziDemoDebug`。
- 注意：基线 profile 与截图测试在不同 OS/环境上行为可能不同；CI（Linux）常作为基准录制环境。

---

## 关键文件与定位
- 顶层：`README.md` / `README_zh.md`（已翻译）
- 应用：`app/src/main/...`
  - `NiaApplication.kt`（应用入口）
  - `MainActivity.kt`（Activity 与主题/edge-to-edge 管理）
  - `MainActivityViewModel.kt`（用户设置与 uiState）
  - `ui/NiaAppState.kt`（rememberNiaAppState、NiaAppState）
  - `ui/NiaApp.kt`（顶层 Compose 入口）
  - `navigation/TopLevelNavItem.kt`（顶层导航定义）
  - `di/JankStatsModule.kt`（JankStats DI）
  - `util/ProfileVerifierLogger.kt`（基线 profile 日志）

- 核心模块：`core/` 下包含 data、network、designsystem、navigation、等通用库。
- feature 模块：`feature/foryou`、`feature/bookmarks`、`feature/interests`、`feature/search`、`feature/topic` 等。

---

## 常见改动点（建议）
- 要增加新的顶层页面：在 `navigation/TopLevelNavItem.kt` 添加新的 `NavKey` 映射，并在 `entryProvider` 中注册对应的 feature entry。
- 要添加新的数据源或未读计算：修改或扩展 `UserNewsResourceRepository`，并将结果组合到 `NiaAppState.topLevelNavKeysWithUnreadResources`。
- 要添加新的全局依赖：向 Hilt 的相应 module（app/di 或 feature 的 module）添加提供函数。

---

## 小结
- 启动到 UI 的主线：Application(init) -> MainActivity(theme+state) -> rememberNiaAppState -> NiaApp (背景/导航) -> NavDisplay -> feature entries。
- 核心关注点在于：导航（NavKey/entryProvider）、应用状态（NiaAppState）、数据仓库（UserNewsResourceRepository / UserDataRepository）和 DI（Hilt 模块）。

如果你需要，我可以：
- 把这份文档进一步展开为每个文件的调用时序图（mermaid），或
- 生成一个更精细的“修改指南”列出修改点和对应代码位置（带精确文件/函数引用）。

