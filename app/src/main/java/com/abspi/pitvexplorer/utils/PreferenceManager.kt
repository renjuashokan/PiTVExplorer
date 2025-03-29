package com.abspi.pitvexplorer.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "PiTVExplorerPrefs",
        Context.MODE_PRIVATE
    )

    companion object {
        const val SERVER_IP_KEY = "server_ip"
    }

    fun saveServerIp(serverIp: String) {
        preferences.edit().putString(SERVER_IP_KEY, serverIp).apply()
    }

    fun getLastServerIp(): String? {
        return preferences.getString(SERVER_IP_KEY, null)
    }
}