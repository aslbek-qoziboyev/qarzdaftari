package com.qarzdaftari.aslbek.data.session

import android.content.Context
import android.content.SharedPreferences
import com.qarzdaftari.aslbek.data.model.User

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "qarzdaftari_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        val GUEST_USER = User(
            id = "guest_user_default",
            name = "Mehmon foydalanuvchi",
            email = "mehmon@qarzdaftari.local"
        )
    }

    fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getUser(): User? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "Foydalanuvchi") ?: "Foydalanuvchi"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        return User(id = id, name = name, email = email)
    }

    fun loginAsGuest(): User {
        saveUser(GUEST_USER)
        return GUEST_USER
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
}
