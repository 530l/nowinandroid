# Now in Android 项目
- 本项目使用 git 托管，仓库地址为：https://github.com/android/nowinandroid

## 版本控制与代码位置

- 截图测试由 CI 生成，因此不应从工作站直接将它们提交到仓库。
- 工作流定义在 `.github/workflows/*.yaml` 中，包含各种检查。

## 持续集成（CI）

- 使用 `google/truth` 进行断言。
- 使用 `cashapp/turbine` 进行复杂的协程测试。
- 使用 `kotlinx.coroutines` 进行大多数断言。

#### 本地测试

- 更大范围的测试位于 `:app` 模块，它们可以启动诸如 `MainActivity` 的 Activity。
- UI 功能测试只应使用带有 `ComponentActivity` 的 `ComposeTestRule`。

#### 仪器化测试

### 创建测试

  `./gradlew pixel6api31aospDebugAndroidTest`。另外还有 `pixel4api30aospatdDebugAndroidTest` 和 `pixelcapi30aospatdDebugAndroidTest`。
- 可以使用 Gradle 管理的设备来运行设备端测试，例如：

### 仪器化测试（Instrumented tests）

- 运行本地截图测试：`./gradlew verifyRoborazziDemoDebug`。
- 运行单个测试：`./gradlew {variant}Test --tests "com.example.myapp.MyTestClass"`。
- 运行本地测试：`./gradlew {variant}Test`。
- 自动修复格式化/代码风格：`./gradlew --init-script gradle/init.gradle.kts spotlessApply`。
- 构建：`./gradlew assemble{Variant}`，通常为 `assembleDemoDebug`。

应用和 Android 库包含两个产品风味（product flavors）：`demo` 与 `prod`，以及两个构建类型（build types）：`debug` 与 `release`。

## 构建与测试命令

主应用位于 `app/` 文件夹。功能模块位于 `feature/`，核心与共享模块位于 `core/`。

## 模块

- **后台任务：** 使用 WorkManager 处理可延迟执行的后台任务。
  - **远程数据：** 使用 Retrofit 与 OkHttp 从网络获取数据。
  - **本地数据：** 使用 Room 和 DataStore 进行本地持久化。
- **数据层：** 采用仓库模式（repository pattern）。
- **导航：** 使用 Jetpack Navigation for Compose 提供声明式且类型安全的屏幕跳转。
- **依赖注入：** 使用 Hilt 进行依赖注入，简化依赖管理并改善可测试性。
- **状态管理：** 采用单向数据流（Unidirectional Data Flow，UDF），使用 Kotlin 协程和 Flow 实现。`ViewModel` 作为状态持有者，以数据流的形式向 UI 暴露状态。
- **UI：** 完全使用 Jetpack Compose 构建，包含 Material 3 组件，并针对不同屏幕尺寸使用自适应布局。

这是一个遵循 Google 官方架构指南的现代 Android 应用。它是一个响应式（reactive）、单 Activity 应用，采用以下技术：

## 架构

Now in Android 是一个使用 Kotlin 编写的原生 Android 移动应用。它会定期提供有关 Android 开发的新闻。用户可以选择关注主题、在有新内容时收到通知，并将条目加入书签。


