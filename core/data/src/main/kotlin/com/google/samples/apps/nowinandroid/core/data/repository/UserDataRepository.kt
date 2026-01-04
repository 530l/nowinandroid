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

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import kotlinx.coroutines.flow.Flow

/**
 * 干嘛用的（大白话版）：
 * 给界面提供用户的设置和偏好（主题、关注话题、收藏、已读等），
 * 并提供修改这些设置的方法。
 */
interface UserDataRepository {

    /**
     * 用户数据流，会持续发最新的 `UserData`。
     * UI 订阅它来决定主题、显示哪些内容等。
     */
    val userData: Flow<UserData>

    /**
     * 一次性把关注的话题列表设置为传入的数据（覆盖式）。
     */
    suspend fun setFollowedTopicIds(followedTopicIds: Set<String>)

    /**
     * 开/关 单个话题的关注状态。
     */
    suspend fun setTopicIdFollowed(followedTopicId: String, followed: Boolean)

    /**
     * 给某条新闻加/取消书签。
     */
    suspend fun setNewsResourceBookmarked(newsResourceId: String, bookmarked: Boolean)

    /**
     * 标记某条新闻为已读或未读。
     */
    suspend fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean)

    /**
     * 设置主题风格（默认或 Android 品牌主题）。
     */
    suspend fun setThemeBrand(themeBrand: ThemeBrand)

    /**
     * 设置暗色主题偏好：跟随系统 / 强制亮 / 强制暗。
     */
    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig)

    /**
     * 开/关 是否使用动态配色（Material You）。
     */
    suspend fun setDynamicColorPreference(useDynamicColor: Boolean)

    /**
     * 设置是否隐藏入门指引（onboarding）。
     */
    suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean)
}
