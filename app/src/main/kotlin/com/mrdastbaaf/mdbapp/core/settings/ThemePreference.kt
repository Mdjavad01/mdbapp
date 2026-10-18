package com.mrdastbaaf.mdbapp.core.settings

/** What the user chose in Settings. [SYSTEM] follows the device. */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStored(raw: String?): ThemePreference = when (raw?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }

        fun storedValue(preference: ThemePreference): String = when (preference) {
            ThemePreference.LIGHT -> "light"
            ThemePreference.DARK -> "dark"
            ThemePreference.SYSTEM -> "system"
        }
    }
}
