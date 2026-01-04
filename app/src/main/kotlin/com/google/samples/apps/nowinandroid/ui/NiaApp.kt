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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaGradientBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationSuiteScaffold
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopAppBar
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.GradientColors
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalGradientColors
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.core.navigation.toEntries
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation.LocalSnackbarHostState
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation.bookmarksEntry
import com.google.samples.apps.nowinandroid.feature.foryou.api.navigation.ForYouNavKey
import com.google.samples.apps.nowinandroid.feature.foryou.impl.navigation.forYouEntry
import com.google.samples.apps.nowinandroid.feature.interests.impl.navigation.interestsEntry
import com.google.samples.apps.nowinandroid.feature.search.api.navigation.SearchNavKey
import com.google.samples.apps.nowinandroid.feature.search.impl.navigation.searchEntry
import com.google.samples.apps.nowinandroid.feature.settings.impl.SettingsDialog
import com.google.samples.apps.nowinandroid.feature.topic.impl.navigation.topicEntry
import com.google.samples.apps.nowinandroid.navigation.TOP_LEVEL_NAV_ITEMS
import com.google.samples.apps.nowinandroid.feature.settings.impl.R as settingsR

// 这个文件包含应用的顶级 Composable：
// - 负责整体背景与渐变色的展示
// - 管理离线提示（snackbar）与设置弹窗
// - 渲染底部/侧边导航与内容区域

@Composable
fun NiaApp(
    appState: NiaAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // 当导航到 "For You" 页时显示渐变背景，否则显示默认背景色
    val shouldShowGradientBackground = appState.navigationState.currentTopLevelKey == ForYouNavKey
    // 控制设置对话框是否显示（状态可保存）
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    // 根背景（用于处理屏幕背景色或图片）
    NiaBackground(modifier = modifier) {
        // 可根据当前顶层栏目选择是否使用渐变色
        NiaGradientBackground(
            gradientColors = if (shouldShowGradientBackground) {
                LocalGradientColors.current
            } else {
                GradientColors()
            },
        ) {
            // 用于显示离线/提示信息的 Snackbar 状态
            val snackbarHostState = remember { SnackbarHostState() }

            val isOffline by appState.isOffline.collectAsStateWithLifecycle()

            // 若检测到离线，则弹出一个长期显示的 snackbar 提示用户
            val notConnectedMessage = stringResource(R.string.not_connected)
            LaunchedEffect(isOffline) {
                if (isOffline) {
                    snackbarHostState.showSnackbar(
                        message = notConnectedMessage,
                        duration = Indefinite,
                    )
                }
            }

            // 将 snackbarHostState 暴露给需要的子组件（例如书签模块）
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                // 将具体 UI 渲染委托给内部实现，便于在外面控制一些全局状态
                NiaApp(
                    appState = appState,

                    // TODO: Settings should be a dialog screen
                    showSettingsDialog = showSettingsDialog,
                    onSettingsDismissed = { showSettingsDialog = false },
                    onTopAppBarActionClick = { showSettingsDialog = true },
                    windowAdaptiveInfo = windowAdaptiveInfo,
                )
            }
        }
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun NiaApp(
    appState: NiaAppState,
    showSettingsDialog: Boolean,
    onSettingsDismissed: () -> Unit,
    onTopAppBarActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // 监听哪些顶层导航项有未读内容（用于显示小红点）
    val unreadNavKeys by appState.topLevelNavKeysWithUnreadResources
        .collectAsStateWithLifecycle()

    // 如果需要展示设置对话框，则渲染它
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { onSettingsDismissed() },
        )
    }

    val snackbarHostState = LocalSnackbarHostState.current

    // 本地 Navigator，负责触发导航动作
    val navigator = remember { Navigator(appState.navigationState) }

    // 导航容器：负责渲染导航项（底部或侧边栏）并显示内容
    NiaNavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                val hasUnread = unreadNavKeys.contains(navKey)
                val selected = navKey == appState.navigationState.currentTopLevelKey
                item(
                    selected = selected,
                    onClick = { navigator.navigate(navKey) },
                    icon = {
                        Icon(
                            imageVector = navItem.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = navItem.selectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(navItem.iconTextId)) },
                    modifier = Modifier
                        .testTag("NiaNavItem")
                        .then(if (hasUnread) Modifier.notificationDot() else Modifier),
                )
            }
        },
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        Scaffold(
            modifier = modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime,
                        ),
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                // 仅在顶层目的地显示顶部 AppBar
                var shouldShowTopAppBar = false

                if (appState.navigationState.currentKey in appState.navigationState.topLevelKeys) {
                    shouldShowTopAppBar = true

                    val destination = TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                        ?: error("Top level nav item not found for ${appState.navigationState.currentTopLevelKey}")

                    NiaTopAppBar(
                        titleRes = destination.titleTextId,
                        navigationIcon = NiaIcons.Search,
                        navigationIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_impl_top_app_bar_navigation_icon_description,
                        ),
                        actionIcon = NiaIcons.Settings,
                        actionIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_impl_top_app_bar_action_icon_description,
                        ),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        onActionClick = { onTopAppBarActionClick() },
                        onNavigationClick = { navigator.navigate(SearchNavKey) },
                    )
                }

                Box(
                    // Workaround for https://issuetracker.google.com/338478720
                    modifier = Modifier.consumeWindowInsets(
                        if (shouldShowTopAppBar) {
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        } else {
                            WindowInsets(0, 0, 0, 0)
                        },
                    ),
                ) {
                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                    val entryProvider = entryProvider {
                        forYouEntry(navigator)
                        bookmarksEntry(navigator)
                        interestsEntry(navigator)
                        topicEntry(navigator)
                        searchEntry(navigator)
                    }

                    // NavDisplay 将 entryProvider 转换为可渲染的 entries，并根据屏幕尺寸选择展示策略
                    NavDisplay(
                        entries = appState.navigationState.toEntries(entryProvider),
                        sceneStrategy = listDetailStrategy,
                        onBack = { navigator.goBack() },
                    )
                }

                // TODO: We may want to add padding or spacer when the snackbar is shown so that
                //  content doesn't display behind it.
            }
        }
    }
}

// 小红点（通知）绘制修饰器：在导航图标上绘制一个小圆点表示未读
private fun Modifier.notificationDot(): Modifier =
    composed {
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        drawWithContent {
            drawContent()
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                // 这里的位移根据 NavigationBar 的指示器 pill 的大小估算得出；
                // 由于相关参数为私有，这里使用一个经验值来定位小点。
                // (NavigationBarTokens.ActiveIndicatorWidth = 64.dp)
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }
