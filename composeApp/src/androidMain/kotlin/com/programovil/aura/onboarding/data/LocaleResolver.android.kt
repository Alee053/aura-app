package com.programovil.aura.onboarding.data

import java.util.Locale

internal actual fun getSystemLocale(): String {
    val language = Locale.getDefault().language
    return if (language in SUPPORTED_LOCALES) language else "en"
}