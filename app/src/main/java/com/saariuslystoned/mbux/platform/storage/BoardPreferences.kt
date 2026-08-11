package com.saariuslystoned.mbux.platform.storage

import android.content.SharedPreferences

interface BoardPreferences {
    fun getString(key: String): String?

    fun putString(key: String, value: String)
}

class SharedPreferencesBoardPreferences(
    private val preferences: SharedPreferences,
) : BoardPreferences {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
