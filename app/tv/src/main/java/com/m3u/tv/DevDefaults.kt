package com.m3u.tv

object DevDefaults {
    val XTREAM_TITLE: String? get() = BuildConfig.DEV_XTREAM_TITLE.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_BASIC_URL: String? get() = BuildConfig.DEV_XTREAM_BASIC_URL.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_USERNAME: String? get() = BuildConfig.DEV_XTREAM_USERNAME.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
    val XTREAM_PASSWORD: String? get() = BuildConfig.DEV_XTREAM_PASSWORD.takeIf { BuildConfig.DEBUG && it.isNotEmpty() }
}
