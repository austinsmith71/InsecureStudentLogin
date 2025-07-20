package com.example.insecurestudentlogin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences
import androidx.core.content.edit

object SecurityHelper{

    private lateinit var prefs: SharedPreferences // will actually return an Shared Pref obj but will be encrypted

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveString(key: String, value: String) {
        prefs.edit {
            putString(key, value)
        }
    }

    fun saveToken(token: String) {
        prefs.edit {
            putString("auth_token", token)
            putBoolean("is_logged_out", false)
        }
    }
    // Clraers the token on a logout
    fun clearTokenAndLogout() {
        prefs.edit {
            remove("auth_token")
            putBoolean("is_logged_out", true)
            saveString("username", "TEST")
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit {
            putBoolean(key, value)
        }
    }

    fun getString(key: String): String? = prefs.getString(key, null)
}
