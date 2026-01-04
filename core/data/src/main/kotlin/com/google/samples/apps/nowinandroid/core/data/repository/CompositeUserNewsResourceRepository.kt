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
import com.google.samples.apps.nowinandroid.core.model.data.mapToUserNewsResources
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 简单说明（中文，大白话）：
 * 这个类把新闻内容（NewsRepository）和用户设置（UserDataRepository）合并起来，
 * 然后给 UI 提供「带用户信息的新闻条目」流（比如标记了是否已读、是否已收藏等）。
 */
class CompositeUserNewsResourceRepository @Inject constructor(
    val newsRepository: NewsRepository,
    val userDataRepository: UserDataRepository,
) : UserNewsResourceRepository {

    /**
     * 返回符合查询条件的新闻项，并把用户数据（已读/已收藏/用户关注等）合并进去。
     * 用处：列表展示时订阅这个流，会拿到带用户状态的新闻项。
     */
    override fun observeAll(
        query: NewsResourceQuery,
    ): Flow<List<UserNewsResource>> =
        newsRepository.getNewsResources(query)
            .combine(userDataRepository.userData) { newsResources, userData ->
                newsResources.mapToUserNewsResources(userData)
            }

    /**
     * 返回用户关注的话题对应的新闻项（带用户状态）。
     * 如果用户没有关注任何话题，直接返回空列表流，避免不必要的查询。
     */
    override fun observeAllForFollowedTopics(): Flow<List<UserNewsResource>> =
        userDataRepository.userData.map { it.followedTopics }.distinctUntilChanged()
            .flatMapLatest { followedTopics ->
                when {
                    followedTopics.isEmpty() -> flowOf(emptyList())
                    else -> observeAll(NewsResourceQuery(filterTopicIds = followedTopics))
                }
            }

    /**
     * 返回用户已收藏（书签）的新闻项（带用户状态）。
     * 如果没有收藏任何内容，直接返回空列表流。
     */
    override fun observeAllBookmarked(): Flow<List<UserNewsResource>> =
        userDataRepository.userData.map { it.bookmarkedNewsResources }.distinctUntilChanged()
            .flatMapLatest { bookmarkedNewsResources ->
                when {
                    bookmarkedNewsResources.isEmpty() -> flowOf(emptyList())
                    else -> observeAll(NewsResourceQuery(filterNewsIds = bookmarkedNewsResources))
                }
            }
}
