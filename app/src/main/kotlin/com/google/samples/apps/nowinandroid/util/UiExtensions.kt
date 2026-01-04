/*
 * Copyright 2024 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.util

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.core.util.Consumer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/*
 简短说明（中文）：
 这个文件包含与 UI 相关的便捷扩展函数，当前提供了判断系统是否处于暗色模式的工具：
 - `Configuration.isSystemInDarkTheme`：在任何 `Configuration` 实例上检查暗色模式。
 - `ComponentActivity.isSystemInDarkTheme()`：返回一个 Flow，订阅后会立刻发出当前暗色模式状态，并在配置变化时继续发出更新。
*/

/**
 * 判断当前 Configuration 是否为暗色主题（简洁包装）。
 */
val Configuration.isSystemInDarkTheme
    get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * 监听 Activity 的配置变更以获得暗色模式状态。
 *
 * 行为要点（简短）：
 * - 订阅时会立即发送当前值（方便 UI 立即响应）。
 * - 注册一个配置变更监听器，配置改变时发送新值。
 * - 在流关闭时自动移除监听器，避免泄漏。
 * - 使用 `distinctUntilChanged()` 避免重复发送相同状态；使用 `conflate()` 在高频更新时只保留最新值。
 */
fun ComponentActivity.isSystemInDarkTheme() = callbackFlow {
    // 先发送当前配置的暗色模式状态
    channel.trySend(resources.configuration.isSystemInDarkTheme)

    // 当系统配置变化时，向 channel 发送新的暗色模式状态
    val listener = Consumer<Configuration> {
        channel.trySend(it.isSystemInDarkTheme)
    }

    // 注册监听器
    addOnConfigurationChangedListener(listener)

    // 取消订阅时移除监听器，防止内存泄漏
    awaitClose { removeOnConfigurationChangedListener(listener) }
}
    // 只在状态发生变化时才发射，减少无效重复
    .distinctUntilChanged()
    // 在高频更新时合并中间值，仅保留最新一次，避免消费端处理积压
    .conflate()
