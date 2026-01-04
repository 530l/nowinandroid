/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import kotlinx.coroutines.flow.Flow

/**
 * 干啥用的（大白话）：
 * 给界面提供新闻列表，而且每条新闻里已经替你标好了「这条是不是你看过／收藏／和你关注的话题有关」。
 * UI 只要订阅这些流，就能直接渲染带用户状态的新闻项。
 */
interface UserNewsResourceRepository {
    /**
     * 根据条件拿新闻列表。
     * 比如按话题过滤或按 id 列表过滤。UI 用这个来显示普通列表。
     */
    fun observeAll(
        query: NewsResourceQuery = NewsResourceQuery(
            filterTopicIds = null,
            filterNewsIds = null,
        ),
    ): Flow<List<UserNewsResource>>

    /**
     * 拿用户关注的话题相关的新闻（也带上用户的已读/收藏状态）。
     * 用处：为你推荐 / 关注 页面的数据源。
     */
    fun observeAllForFollowedTopics(): Flow<List<UserNewsResource>>

    /**
     * 拿用户收藏（书签）的新闻（也带上用户的已读状态）。
     * 用处：书签页或计算书签相关的未读提醒。
     */
    fun observeAllBookmarked(): Flow<List<UserNewsResource>>
}
