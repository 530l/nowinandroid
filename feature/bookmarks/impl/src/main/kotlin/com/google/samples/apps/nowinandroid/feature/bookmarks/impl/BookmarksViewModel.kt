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

package com.google.samples.apps.nowinandroid.feature.bookmarks.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 大白话（文件顶层说明）：
// 这个 ViewModel 管理“书签”页的数据和撤销逻辑。
// 新手看这里：UI 会订阅 `feedUiState` 来渲染列表；当用户取消书签时，ViewModel 会保存被移除的 id 并展示撤销提示。
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {

    // 是否应该显示“撤销”提示（会被 UI 观察）。
    var shouldDisplayUndoBookmark by mutableStateOf(false)

    // 最近一次被移除的书签 id（用于撤销操作），没有则为 null。
    private var lastRemovedBookmarkId: String? = null

    ////////////////////////////////////////////////////////////////////////////////////////////
    // 供 UI 订阅的 feed 状态：先发 Loading，然后发 Success(列表)
    // val feedState by viewModel.feedUiState.collectAsStateWithLifecycle() 开始收集的时候，就会开启冷流
    // UI 订阅这个 StateFlow，就能拿到用户收藏的新闻列表。

    //UI 一进入页面，立刻显示 Loading；数据返回后自动刷新为新闻列表；
    // 离开页面 5 秒后自动停止监听；再次进入则重新加载——整个过程自动、安全、高效。
    // 5s (SharingStarted.WhileSubscribed(5_000))
    //started = SharingStarted.WhileSubscribed(5_000) 的意思是：
    //  只要有订阅者（UI 组件）在订阅这个 StateFlow，就保持上游流活跃；
    //  如果 5 秒内没有订阅者了，就停止上游流，节省资源。
    // 这样设计是为了在用户快速切换页面时避免频繁启动/停止流，提升性能和用户体验。
    //

    val feedUiState: StateFlow<NewsFeedUiState> =
        // 获取用户收藏的新闻流（冷流）
        userNewsResourceRepository.observeAllBookmarked()
            // 将新闻列表映射为 Success 状态
            .map<List<UserNewsResource>, NewsFeedUiState>(NewsFeedUiState::Success)
            // 在冷流启动时，先发出 Loading 状态（确保加载过程可见）
            .onStart { emit(Loading) }
            // 转换为 StateFlow，供 UI 订阅
            .stateIn(
                scope = viewModelScope,
                // 仅在有活跃订阅者时启动上游流，5秒无订阅则停止
                started = SharingStarted.WhileSubscribed(5_000),
                // StateFlow 的初始值（在冷流启动前 UI 就能立即显示 Loading）
                initialValue = Loading,
            )

    /**
     * 用户点击取消书签时调用：
     * - 记录被移除的 id
     * - 设置标记以便 UI 弹出撤销 snackbar
     * - 实际更新仓库（移除书签）
     */
    fun removeFromSavedResources(newsResourceId: String) {
        viewModelScope.launch {
            shouldDisplayUndoBookmark = true
            lastRemovedBookmarkId = newsResourceId
            userDataRepository.setNewsResourceBookmarked(newsResourceId, false)
        }
    }

    /**
     * 标记某条新闻为已读/未读（UI 在打开文章后会调用）。
     */
    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    /**
     * 撤销上一次的移除操作：如果有记录的 id，就把书签加回去。
     * 调用后会清理撤销状态。
     */
    fun undoBookmarkRemoval() {
        viewModelScope.launch {
            lastRemovedBookmarkId?.let {
                userDataRepository.setNewsResourceBookmarked(it, true)
            }
        }
        clearUndoState()
    }

    /**
     * 清理撤销相关的临时状态（常在 snackbar 消失或页面离开时调用）。
     */
    fun clearUndoState() {
        shouldDisplayUndoBookmark = false
        lastRemovedBookmarkId = null
    }
}
