/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.JankStats
import androidx.tracing.trace
import com.google.samples.apps.nowinandroid.MainActivityUiState.Loading
import com.google.samples.apps.nowinandroid.core.analytics.AnalyticsHelper
import com.google.samples.apps.nowinandroid.core.analytics.LocalAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.TimeZoneMonitor
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.ui.LocalTimeZone
import com.google.samples.apps.nowinandroid.ui.NiaApp
import com.google.samples.apps.nowinandroid.ui.rememberNiaAppState
import com.google.samples.apps.nowinandroid.util.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 简短说明（中文）：
     * MainActivity 是应用的宿主 Activity，负责：
     * - 注入跨应用工具（如 JankStats、网络/时区监控、分析工具）
     * - 控制主题（暗色/动态配色）并开启 edge-to-edge
     * - 创建并提供 `NiaAppState` 给 Composable 树
     */

    /**
     * 懒加载注入的 JankStats，用于性能卡顿统计（按需启停以节省资源）。
     */
    @Inject
    lateinit var lazyStats: dagger.Lazy<JankStats>

    // 注入网络状态监控，用于检测离线
    @Inject
    lateinit var networkMonitor: NetworkMonitor

    // 注入时区监控，用于将当前时区提供给界面展示时间
    @Inject
    lateinit var timeZoneMonitor: TimeZoneMonitor

    // 注入 Analytics helper，用于在 UI 层记录事件
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    // 注入用户阅读/书签资源仓库，用于计算未读状态等
    @Inject
    lateinit var userNewsResourceRepository: UserNewsResourceRepository

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 使用一个可变状态对象收集主题相关设置，便于在 Compose 中响应变化。
        // 这个对象合并了系统配置和用户偏好，避免不必要的重组。
        var themeSettings by mutableStateOf(
            ThemeSettings(
                darkTheme = resources.configuration.isSystemInDarkTheme,
                androidTheme = Loading.shouldUseAndroidTheme,
                disableDynamicTheming = Loading.shouldDisableDynamicTheming,
            ),
        )

        // 组合系统暗色模式流与 viewModel.uiState，计算最终的主题设置并在生命周期 STARTED 时收集。
        // 在收集到暗色模式变化时，会调用 enableEdgeToEdge 来开启沉浸式（edge-to-edge）并设置系统栏样式。
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(),
                    viewModel.uiState,
                ) { systemDark, uiState ->
                    ThemeSettings(
                        darkTheme = uiState.shouldUseDarkTheme(systemDark),
                        androidTheme = uiState.shouldUseAndroidTheme,
                        disableDynamicTheming = uiState.shouldDisableDynamicTheming,
                    )
                }
                    .onEach { themeSettings = it }
                    .map { it.darkTheme }
                    .distinctUntilChanged()
                    .collect { darkTheme ->
                        trace("niaEdgeToEdge") {
                            // 开启 edge-to-edge：我们手动决定是否使用暗色主题（基于 uiState），
                            // 并保持与默认 enableEdgeToEdge 相同的透明遮罩参数。
                            enableEdgeToEdge(
                                statusBarStyle = SystemBarStyle.auto(
                                    lightScrim = android.graphics.Color.TRANSPARENT,
                                    darkScrim = android.graphics.Color.TRANSPARENT,
                                ) { darkTheme },
                                navigationBarStyle = SystemBarStyle.auto(
                                    lightScrim = lightScrim,
                                    darkScrim = darkScrim,
                                ) { darkTheme },
                            )
                        }
                    }
            }
        }

        // 保持启动屏直到 UI 状态加载完成，避免在数据加载前展示空白界面。
        // 这个判断会在每次重绘时被快速评估，因此应保持轻量。
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.shouldKeepSplashScreen() }

        setContent {
            // 创建并记住应用级状态（NiaAppState），该状态暴露网络/未读/时区等信息给 UI
            val appState = rememberNiaAppState(
                networkMonitor = networkMonitor,
                userNewsResourceRepository = userNewsResourceRepository,
                timeZoneMonitor = timeZoneMonitor,
            )

            // 从 appState 订阅当前时区，用于在界面中本地化时间显示
            val currentTimeZone by appState.currentTimeZone.collectAsStateWithLifecycle()

            // 将分析工具与当前时区通过 CompositionLocal 提供给下层 Composable
            CompositionLocalProvider(
                LocalAnalyticsHelper provides analyticsHelper,
                LocalTimeZone provides currentTimeZone,
            ) {
                // 应用主题：根据之前计算的 themeSettings 决定暗色/动态配色等
                NiaTheme(
                    darkTheme = themeSettings.darkTheme,
                    androidTheme = themeSettings.androidTheme,
                    disableDynamicTheming = themeSettings.disableDynamicTheming,
                ) {
                    // 应用的顶层 Composable
                    NiaApp(appState)
                }
            }
        }
    }

    // 启用/禁用 JankStats 跟踪，避免在后台也持续消耗资源
    override fun onResume() {
        super.onResume()
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        lazyStats.get().isTrackingEnabled = false
    }
}

/**
 * The default light scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
// 透明度较高的浅色遮罩，用于在浅色模式下对导航栏或状态栏做微弱遮罩
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * The default dark scrim, as defined by androidx and the platform:
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 */
// 暗色模式下使用的暗色遮罩
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Class for the system theme settings.
 * This wrapping class allows us to combine all the changes and prevent unnecessary recompositions.
 */
// 主题设置数据类：封装暗色/原生主题/是否禁用动态配色等选项，减少重组频率
data class ThemeSettings(
    val darkTheme: Boolean,
    val androidTheme: Boolean,
    val disableDynamicTheming: Boolean,
)
