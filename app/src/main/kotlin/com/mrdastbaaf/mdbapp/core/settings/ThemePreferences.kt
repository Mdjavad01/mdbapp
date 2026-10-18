package com.mrdastbaaf.mdbapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The delegate must be top-level: `preferencesDataStore` creates one
 * `DataStore` per file per process, and holding it in a class would create a
 * second instance on every injection and throw at runtime.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mdb_settings",
)

/**
 * User preferences that outlive a session.
 *
 * Kept separate from `SecureTokenStore` on purpose: a theme choice is not a
 * secret, and putting it behind the Keystore would make reading it on every
 * cold start needlessly expensive.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val themeKey = stringPreferencesKey("theme_mode")

    /** Defaults to [ThemePreference.SYSTEM] — follow the phone until told otherwise. */
    val theme: Flow<ThemePreference> = context.settingsDataStore.data.map { preferences ->
        ThemePreference.fromStored(preferences[themeKey])
    }

    suspend fun setTheme(preference: ThemePreference) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeKey] = ThemePreference.storedValue(preference)
        }
    }
}
