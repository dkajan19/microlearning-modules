package com.example.myapplication

import android.content.SharedPreferences

object TokenManager {

    fun getAccessToken(sharedPreferences: SharedPreferences): String? {
        val accessToken = sharedPreferences.getString("access_token", null)
        val expiresIn = sharedPreferences.getLong("expires_in", 0L)
        return if (System.currentTimeMillis() / 1000 < expiresIn) accessToken else null
    }

    fun saveTokens(response: LoginResponse, sharedPreferences: SharedPreferences) {
        with(sharedPreferences.edit()) {
            putString("access_token", response.access_token)
            putLong("expires_in", System.currentTimeMillis() / 1000 + response.expires_in)
            apply()
        }
    }

    fun clearTokens(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().clear().apply()
    }
}
