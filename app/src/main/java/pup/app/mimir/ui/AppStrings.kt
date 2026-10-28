package pup.app.mimir.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** Resolves localised strings outside composition, for status messages and progress labels. */
class AppStrings(private val context: Context) {
    fun text(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        context.resources.getQuantityString(id, count, *args)
}
