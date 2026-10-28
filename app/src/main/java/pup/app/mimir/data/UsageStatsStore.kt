package pup.app.mimir.data

import android.content.Context
import pup.app.mimir.domain.ToolUsage
import pup.app.mimir.domain.UsageStats

/** Keeps [UsageStats] in its own preferences file, so it survives restarts and app updates. */
class UsageStatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("mimir_stats", Context.MODE_PRIVATE)

    @Synchronized
    fun load(): UsageStats {
        val since = prefs.getLong(KEY_SINCE, 0L).takeIf { it > 0L } ?: System.currentTimeMillis().also {
            prefs.edit().putLong(KEY_SINCE, it).apply()
        }
        val tools = prefs.all.keys
            .filter { it.startsWith(GAMES_PREFIX) }
            .associate { key ->
                val tool = key.removePrefix(GAMES_PREFIX)
                tool to ToolUsage(
                    games = prefs.getInt(key, 0),
                    spaceSavedBytes = prefs.getLong(SAVED_PREFIX + tool, 0L),
                )
            }
        return UsageStats(sinceMillis = since, tools = tools)
    }

    /** Adds one run's results to the stored totals and returns the new totals. */
    @Synchronized
    fun record(key: String, games: Int, spaceSavedBytes: Long): UsageStats {
        val updated = load().record(key, games, spaceSavedBytes)
        val usage = updated.usage(key)
        prefs.edit()
            .putInt(GAMES_PREFIX + key, usage.games)
            .putLong(SAVED_PREFIX + key, usage.spaceSavedBytes)
            .apply()
        return updated
    }

    private companion object {
        const val KEY_SINCE = "since"
        const val GAMES_PREFIX = "games_"
        const val SAVED_PREFIX = "saved_"
    }
}
