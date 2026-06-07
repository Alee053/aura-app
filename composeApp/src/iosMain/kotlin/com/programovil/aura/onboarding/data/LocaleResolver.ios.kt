package com.programovil.aura.onboarding.data

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

internal actual fun getSystemLocale(): String {
    val language = NSLocale.currentLocale.languageCode
    return if (language in SUPPORTED_LOCALES) language else "en"
}