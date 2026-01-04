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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.MainActivityUiState.Loading
import com.google.samples.apps.nowinandroid.MainActivityUiState.Success
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ViewModel：从仓库读取用户设置并公开给 Activity/Compose 使用
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {
    // uiState 是一个 StateFlow，表示当前 Activity 需要的用户设置加载状态：
    // - Loading：尚未加载完用户设置（通常用于保持启动屏）
    // - Success：已加载，携带 UserData，可用于决定主题/配色等
    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )
}

/**
 * Activity 使用的 UI 状态封装。
 * 简化说明：
 * - 在数据未加载时为 `Loading`，加载完成后为 `Success(UserData)`。
 * - 提供若干辅助属性/方法，帮助决定主题、动态配色和是否需要继续展示启动屏。
 */
sealed interface MainActivityUiState {
    // 数据尚未准备好时的占位状态
    data object Loading : MainActivityUiState

    // 成功加载到用户数据后的状态
    data class Success(val userData: UserData) : MainActivityUiState {
        // 是否禁用动态配色：当用户选择不使用动态颜色时为 true
        override val shouldDisableDynamicTheming = !userData.useDynamicColor

        // 是否使用 Android 原生主题（例如以 Android 品牌色为主）
        override val shouldUseAndroidTheme: Boolean = when (userData.themeBrand) {
            ThemeBrand.DEFAULT -> false
            ThemeBrand.ANDROID -> true
        }

        // 是否使用暗色主题：根据用户偏好决定（跟随系统/强制亮/强制暗）
        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) =
            when (userData.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemDarkTheme
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }
    }

    /**
     * 如果还在 Loading 状态，返回 true，会用于决定是否继续显示启动屏。
     */
    fun shouldKeepSplashScreen() = this is Loading

    /**
     * 动态配色是否被禁用（默认实现：启用）。
     * Success 会覆盖此值以反映用户设置。
     */
    val shouldDisableDynamicTheming: Boolean get() = true

    /**
     * 是否使用系统/Android 风格主题（默认 false）。
     * Success 会覆盖此值以反映用户选择。
     */
    val shouldUseAndroidTheme: Boolean get() = false

    /**
     * 根据系统暗色模式判断是否应使用暗色主题（默认：跟随系统）。
     */
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}
