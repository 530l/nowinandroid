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

package com.google.samples.apps.nowinandroid.di

import android.app.Activity
import android.util.Log
import android.view.Window
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

// 该模块为 Activity 范围提供与 JankStats 相关的依赖（用于检测并记录 UI 卡顿帧）
@Module
@InstallIn(ActivityComponent::class)
object JankStatsModule {
    // 提供一个 OnFrameListener：只记录“卡顿”帧（isJank == true），当前将其写入日志，
    // 实际生产环境更建议把这些数据上报到后端或采集系统以便分析。
    @Provides
    fun providesOnFrameListener(): OnFrameListener = OnFrameListener { frameData ->
        // Make sure to only log janky frames.
        // 确保只记录卡顿帧。
        if (frameData.isJank) {
            // We're currently logging this but would better report it to a backend.
            // 我们目前正在记录此问题，但最好将其报告给后端。
            Log.v("NiA Jank", frameData.toString())
        }
    }

    // 从 Activity 提供 Window 实例，供 JankStats.createAndTrack 使用。
    @Provides
    fun providesWindow(activity: Activity): Window = activity.window

    // 创建并启动 JankStats 跟踪：将 window 与 frameListener 绑定，返回可注入的 JankStats 实例。
    @Provides
    fun providesJankStats(
        window: Window,
        frameListener: OnFrameListener,
    ): JankStats = JankStats.createAndTrack(window, frameListener)
}
