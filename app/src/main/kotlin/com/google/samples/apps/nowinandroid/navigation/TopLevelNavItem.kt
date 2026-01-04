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

package com.google.samples.apps.nowinandroid.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.navigation.BookmarksNavKey
import com.google.samples.apps.nowinandroid.feature.foryou.api.navigation.ForYouNavKey
import com.google.samples.apps.nowinandroid.feature.interests.api.navigation.InterestsNavKey
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.R as bookmarksR
import com.google.samples.apps.nowinandroid.feature.foryou.api.R as forYouR
import com.google.samples.apps.nowinandroid.feature.search.api.R as searchR

/**
 * 顶层导航项的数据类型（Top level navigation item）。
 *
 * 包含在应用顶部栏和通用导航 UI 中所需的 UI 信息，用于描述某一导航目标的图标及文本。
 *
 * @param selectedIcon 选中该目的地时在导航 UI 中显示的图标。
 * @param unselectedIcon 未选中时显示的图标。
 * @param iconTextId 导航栏中图标下方或旁边显示的文本资源 id（用于简短标签）。
 * @param titleTextId 顶部应用栏显示的标题文本资源 id（用于更长的标题文本）。
 */
data class TopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

// 以下是应用的三个顶层导航项的具体定义。
// 每个常量都提供了选中 / 未选中图标以及需要展示的文本资源 id。

val FOR_YOU = TopLevelNavItem(
    selectedIcon = NiaIcons.Upcoming,
    unselectedIcon = NiaIcons.UpcomingBorder,
    // "为你推荐" 功能的标题资源（来自 feature:foryou 模块的资源）
    iconTextId = forYouR.string.feature_foryou_api_title,
    // 顶部栏显示应用名称（示例：在选中时显示应用名作为标题）
    titleTextId = R.string.app_name,
)

val BOOKMARKS = TopLevelNavItem(
    selectedIcon = NiaIcons.Bookmarks,
    unselectedIcon = NiaIcons.BookmarksBorder,
    // 书签功能的短标签资源 id（来自 feature:bookmarks 模块）
    iconTextId = bookmarksR.string.feature_bookmarks_api_title,
    // 书签到顶部栏显示其模块标题
    titleTextId = bookmarksR.string.feature_bookmarks_api_title,
)

val INTERESTS = TopLevelNavItem(
    selectedIcon = NiaIcons.Grid3x3,
    unselectedIcon = NiaIcons.Grid3x3,
    // 兴趣/主题筛选的短标签资源 id（来自 feature:search 模块，因为该标签定义在 search 模块的资源中）
    iconTextId = searchR.string.feature_search_api_interests,
    // 顶部栏显示兴趣/主题相关的标题
    titleTextId = searchR.string.feature_search_api_interests,
)

// 将导航 key 映射到对应的 TopLevelNavItem。此映射用于决定在导航组件中展示哪个顶层导航项。
val TOP_LEVEL_NAV_ITEMS = mapOf(
    ForYouNavKey to FOR_YOU,
    BookmarksNavKey to BOOKMARKS,
    // InterestsNavKey 接受一个可选参数（例如：选定的 topic id），此处使用 null 表示通用的兴趣入口
    InterestsNavKey(null) to INTERESTS,
)
