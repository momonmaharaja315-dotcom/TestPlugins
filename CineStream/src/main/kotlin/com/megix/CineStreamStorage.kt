package com.megix

import android.content.Context
import androidx.core.content.edit

object CineStreamStorage {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private val prefs by lazy {
        context.getSharedPreferences("CineStreamPrefs", Context.MODE_PRIVATE)
    }

    fun saveCookie(cookie: String) {
        prefs.edit {
            putString("nf_cookie", cookie)
            putLong("nf_cookie_timestamp", System.currentTimeMillis())
        }
    }

    fun getCookie(): Pair<String?, Long> {
        return Pair(
            prefs.getString("nf_cookie", null),
            prefs.getLong("nf_cookie_timestamp", 0L)
        )
    }
    
    fun clearCookie() {
        prefs.edit {
            remove("nf_cookie")
            remove("nf_cookie_timestamp")
        }
    }
}
