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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.TimeZoneMonitor
import com.google.samples.apps.nowinandroid.core.navigation.NavigationState
import com.google.samples.apps.nowinandroid.core.navigation.rememberNavigationState
import com.google.samples.apps.nowinandroid.core.ui.TrackDisposableJank
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.navigation.BookmarksNavKey
import com.google.samples.apps.nowinandroid.feature.foryou.api.navigation.ForYouNavKey
import com.google.samples.apps.nowinandroid.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

// 创建并记住 NiaApp 的状态对象，用于 UI 层使用
@Composable
fun rememberNiaAppState(
    networkMonitor: NetworkMonitor,
    userNewsResourceRepository: UserNewsResourceRepository,
    timeZoneMonitor: TimeZoneMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): NiaAppState {
    // 初始化导航状态，默认页面为 ForYou，顶层导航键来自 TOP_LEVEL_NAV_ITEMS
    val navigationState = rememberNavigationState(ForYouNavKey, TOP_LEVEL_NAV_ITEMS.keys)

    // 跟踪导航以用于性能统计（JankStats）
    NavigationTrackingSideEffect(navigationState)

    return remember(
        navigationState,
        coroutineScope,
        networkMonitor,
        userNewsResourceRepository,
        timeZoneMonitor,
    ) {
        NiaAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
            networkMonitor = networkMonitor,
            userNewsResourceRepository = userNewsResourceRepository,
            timeZoneMonitor = timeZoneMonitor,
        )
    }
}

// 应用级别的状态容器（可稳定使用），向 UI 暴露需要的状态流
@Stable
class NiaAppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    userNewsResourceRepository: UserNewsResourceRepository,
    timeZoneMonitor: TimeZoneMonitor,
) {
    // 表示是否离线（true = 离线）。从 NetworkMonitor 的 isOnline 取反并转成 StateFlow，方便在 Compose 中订阅。
    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * 哪些顶层导航键有未读内容（用于显示小红点等提示）。
     * 通过组合“关注主题的资源”和“已书签资源”的未读状态来计算。
     */
    val topLevelNavKeysWithUnreadResources: StateFlow<Set<NavKey>> =
        userNewsResourceRepository.observeAllForFollowedTopics()
            .combine(userNewsResourceRepository.observeAllBookmarked()) { forYouNewsResources, bookmarkedNewsResources ->
                setOfNotNull(
                    // 如果关注的资源中有未读，则将 ForYouNavKey 包含进集合
                    ForYouNavKey.takeIf { forYouNewsResources.any { !it.hasBeenViewed } },
                    // 如果书签资源中有未读，则将 BookmarksNavKey 包含进集合
                    BookmarksNavKey.takeIf { bookmarkedNewsResources.any { !it.hasBeenViewed } },
                )
            }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

    // 当前时区信息，默认使用系统时区。供需要显示时间/本地化的 UI 使用。
    val currentTimeZone = timeZoneMonitor.currentTimeZone
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5_000),
            TimeZone.currentSystemDefault(),
        )
}

/**
 * 跟踪导航事件以用于 JankStats（性能/卡顿追踪）。
 * 在导航 key 变化时，将当前 key 写入 metricsHolder 中。
 */
@Composable
private fun NavigationTrackingSideEffect(navigationState: NavigationState) {
    TrackDisposableJank(navigationState.currentKey) { metricsHolder ->
        metricsHolder.state?.putState("Navigation", navigationState.currentKey.toString())
        onDispose {}
    }
}
