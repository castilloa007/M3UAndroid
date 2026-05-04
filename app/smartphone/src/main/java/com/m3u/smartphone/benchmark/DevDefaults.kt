package com.m3u.smartphone.benchmark

import com.m3u.smartphone.BuildConfig

object DevDefaults {
    // Credentials come from local.properties → BuildConfig (never in source)
    // Only active in DEBUG builds
    val XTREAM_TITLE: String? get() = BuildConfig.DEV_XTREAM_TITLE.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_BASIC_URL: String? get() = BuildConfig.DEV_XTREAM_BASIC_URL.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_USERNAME: String? get() = BuildConfig.DEV_XTREAM_USERNAME.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_PASSWORD: String? get() = BuildConfig.DEV_XTREAM_PASSWORD.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
}
