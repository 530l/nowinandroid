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

package com.google.samples.apps.nowinandroid.util

import android.util.Log
import androidx.profileinstaller.ProfileVerifier
import com.google.samples.apps.nowinandroid.core.network.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Logs the app's Baseline Profile Compilation Status using [ProfileVerifier].
 *
 * When delivering through Google Play, the baseline profile is compiled during installation.
 * In this case you will see the correct state logged without any further action necessary.
 * To verify baseline profile installation locally, you need to manually trigger baseline
 * profile installation.
 *
 * For immediate compilation, call:
 * ```bash
 * adb shell cmd package compile -f -m speed-profile com.example.macrobenchmark.target
 * ```
 * You can also trigger background optimizations:
 * ```bash
 * adb shell pm bg-dexopt-job
 * ```
 * Both jobs run asynchronously and might take some time complete.
 *
 * To see quick turnaround of the ProfileVerifier, we recommend using `speed-profile`.
 * If you don't do either of these steps, you might only see the profile status reported as
 * "enqueued for compilation" when running the sample locally.
 *
 * @see androidx.profileinstaller.ProfileVerifier.CompilationStatus.ResultCode
 */

/*
 简短说明（中文）：
 - 该类用于查询并记录应用的基线 profile 是否已被安装/编译。
 - 在 Play 商店分发时，安装过程会自动编译；本地调试时可用上面的 adb 命令手动触发编译以快速验证。
 - 使用方式：在合适的时机（如应用启动或手动触发）调用 `ProfileVerifierLogger()`。
*/
class ProfileVerifierLogger @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "ProfileInstaller"
    }

    operator fun invoke() = scope.launch {
        // 异步获取 ProfileVerifier 返回的编译状态，并记录主要字段以便调试。
        val status = ProfileVerifier.getCompilationStatusAsync().await()
        Log.d(TAG, "Status code: ${status.profileInstallResultCode}")
        Log.d(
            TAG,
            when {
                // 已使用基线 profile 编译
                status.isCompiledWithProfile -> "App compiled with profile"
                // 已排队等待编译（常见于未手动触发本地编译时）
                status.hasProfileEnqueuedForCompilation() -> "Profile enqueued for compilation"
                // 未编译也未排队
                else -> "Profile not compiled nor enqueued"
            },
        )
    }
}
