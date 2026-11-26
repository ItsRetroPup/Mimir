package pup.app.mimir.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * A language Mimir is translated into. [tag] matches a `values-*` folder and `locales_config.xml`;
 * [nativeName] is written in the language itself, so anyone can find their own in the list.
 */
internal data class AppLanguage(val tag: String, val nativeName: String)

internal val appLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("es", "Español"),
    AppLanguage("fr", "Français"),
    AppLanguage("it", "Italiano"),
    AppLanguage("pl", "Polski"),
    AppLanguage("pt-BR", "Português (Brasil)"),
    AppLanguage("ja", "日本語"),
    AppLanguage("ko", "한국어"),
    AppLanguage("zh-CN", "简体中文"),
    AppLanguage("zh-TW", "繁體中文"),
)

/** The language picked in Mimir, or null when it follows the system language. */
internal fun chosenLanguageTag(): String? =
    AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }

/** Switches Mimir to [tag], or back to the system language when null. The activity recreates itself. */
internal fun chooseLanguage(tag: String?) {
    val locales = if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(locales)
}

/**
 * This context, reading resources in the language picked in Mimir. Before Android 13 the choice only
 * reaches activity contexts, so text resolved from the application context needs this to follow it.
 */
internal fun Context.inAppLanguage(): Context {
    val tag = chosenLanguageTag() ?: return this
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList.forLanguageTags(tag))
    return createConfigurationContext(config)
}
