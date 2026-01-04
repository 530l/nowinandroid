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

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.samples.apps.nowinandroid.sync.initializers.Sync
import com.google.samples.apps.nowinandroid.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 应用入口 Application 类（NiaApplication）。
 * 简短说明：初始化应用级服务（例如 Sync、ProfileVerifier 日志记录），并提供 Coil 的 ImageLoader。
 */
@HiltAndroidApp
class NiaApplication : Application(), ImageLoaderFactory {
    // 使用 dagger 的 Lazy 注入 ImageLoader，避免在 Application 创建时立即实例化（按需提供）
    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    // 注入用于记录基线 profile 状态的工具（可在启动时触发检查并记录日志）
    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    override fun onCreate() {
        super.onCreate()

        // 在调试模式下启用严格模式以尽早发现主线程上的潜在问题
        setStrictModePolicy()

        // 初始化数据同步系统，负责保持应用内数据与后端/缓存一致
        Sync.initialize(context = this)
        // 记录/检查基线 profile 的安装/编译状态，便于本地调试和性能验证
        profileVerifierLogger()
    }

    // 返回应用中使用的 ImageLoader 实例（由 dagger 按需提供）
    override fun newImageLoader(): ImageLoader = imageLoader.get()

    /**
     * 返回应用是否处于可调试（debuggable）模式。
     */
    private fun isDebuggable(): Boolean {
        // 通过判断 applicationInfo.flags 中的 FLAG_DEBUGGABLE 位来决定
        return 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    /**
     * 在调试构建中启用 StrictMode 线程策略，帮助在开发阶段捕捉主线程上的磁盘/网络访问等问题。
     * 生产构建不会启用此检测，以避免影响真实用户体验。
     */
    private fun setStrictModePolicy() {
        if (isDebuggable()) {
            StrictMode.setThreadPolicy(
                Builder().detectAll().penaltyLog().build(),
            )
        }
    }
}
