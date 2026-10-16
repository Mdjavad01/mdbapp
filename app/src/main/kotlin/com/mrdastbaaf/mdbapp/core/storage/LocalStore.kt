package com.mrdastbaaf.mdbapp.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-secret local state: caches and preferences.
 *
 * Explicitly NOT encrypted. Everything here is either public data (catalogue
 * responses) or harmless UI state (last tab, dismissed banners). Video URLs,
 * playback tickets and tokens must never be written here — they live in memory
 * for the duration of a playback session and nowhere else.
 */
@Singleton
class LocalStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Last known playback position, in seconds, keyed by lesson id.
     *
     * Held locally until the server exposes `GET/PUT /lessons/{id}/progress`.
     * Once it does, this becomes a write-through cache rather than the source of
     * truth — the server wins on conflict.
     */
    fun lastPositionSeconds(lessonId: String): Long =
        prefs.getLong(keyPosition(lessonId), 0L)

    fun setLastPositionSeconds(lessonId: String, seconds: Long) {
        prefs.edit { putLong(keyPosition(lessonId), seconds.coerceAtLeast(0L)) }
    }

    fun clearPosition(lessonId: String) {
        prefs.edit { remove(keyPosition(lessonId)) }
    }

    /** Cached raw JSON for the catalogue, so the list renders offline. */
    fun cachedCoursesJson(): String? = prefs.getString(KEY_COURSES_CACHE, null)

    fun setCachedCoursesJson(json: String) {
        prefs.edit { putString(KEY_COURSES_CACHE, json) }
    }

    fun lastSyncEpochMillis(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSyncEpochMillis(epochMillis: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC, epochMillis) }
    }

    /** Wipes everything except the auth token, which [SecureTokenStore] owns. */
    fun clear() {
        prefs.edit(commit = true) { clear() }
    }

    private fun keyPosition(lessonId: String) = "position_$lessonId"

    private companion object {
        const val PREFS_NAME = "mdbapp_local_store"
        const val KEY_COURSES_CACHE = "courses_cache_json"
        const val KEY_LAST_SYNC = "last_sync_millis"
    }
}
