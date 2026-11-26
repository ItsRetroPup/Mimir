package pup.app.mimir.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Resolves localised strings outside composition, for status messages and progress labels, in the
 * language picked in Mimir rather than only the system one.
 */
class AppStrings(private val context: Context) {
    // Read from background jobs too, so the language and its context are swapped as one value.
    @Volatile
    private var cached: Pair<String?, Context> = null to context

    private fun localized(): Context {
        val tag = chosenLanguageTag()
        val current = cached
        if (current.first == tag) return current.second
        return context.inAppLanguage().also { cached = tag to it }
    }

    fun text(@StringRes id: Int, vararg args: Any): String = localized().getString(id, *args)

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        localized().resources.getQuantityString(id, count, *args)
}
