/*
 * Copyright 2025 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation

import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.navigation.BookmarksNavKey
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.BookmarksScreen
import com.google.samples.apps.nowinandroid.feature.topic.api.navigation.navigateToTopic

// 注册书签页到导航。用 app 的 Snackbar 来弹消息。
fun EntryProviderScope<NavKey>.bookmarksEntry(navigator: Navigator) {
    entry<BookmarksNavKey> {
        // 拿到 app 提供的全局 Snackbar 状态
        val snackbarHostState = LocalSnackbarHostState.current
        BookmarksScreen(
            onTopicClick = navigator::navigateToTopic,
            onShowSnackbar = { message, action ->
                // 弹短提示，返回用户是否点了操作按钮
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = action,
                    duration = Short,
                ) == ActionPerformed
            },
        )
    }
}

// 全局 Snackbar 的入口（真实实例由 app 在运行时提供）
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState state should be initialized at runtime")
}
