package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import kotlin.reflect.KClass
import okhttp3.FormBody
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay
import android.content.Context
import android.content.SharedPreferences

val JSONParser = object : ResponseParser {
    val mapper: ObjectMapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    ).configure(
        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true
    )

    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return mapper.readValue(text, kClass.java)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            mapper.readValue(text, kClass.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }
}

val app = Requests(responseParser = JSONParser).apply {
    defaultHeaders = mapOf("User-Agent" to USER_AGENT)
}

inline fun <reified T : Any> parseJson(text: String): T {
    return JSONParser.parse(text, T::class)
}

inline fun <reified T : Any> tryParseJson(text: String): T? {
    return try {
        return JSONParser.parseSafe(text, T::class)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun convertRuntimeToMinutes(runtime: String): Int {
    var totalMinutes = 0

    val parts = runtime.split(" ")

    for (part in parts) {
        when {
            part.endsWith("h") -> {
                val hours = part.removeSuffix("h").trim().toIntOrNull() ?: 0
                totalMinutes += hours * 60
            }
            part.endsWith("m") -> {
                val minutes = part.removeSuffix("m").trim().toIntOrNull() ?: 0
                totalMinutes += minutes
            }
        }
    }

    return totalMinutes
}

data class VerifyUrl(
    val url: String
)

suspend fun bypass(context: Context, mainUrl: String): String {
    val savedCookie = CineStreamPrefs.getCookie(context)
    val lastUpdated = CineStreamPrefs.getLastUpdate(context)
    val currentTime = System.currentTimeMillis()
    val twentyFourHours = 24 * 60 * 60 * 1000

    if (savedCookie.isNotEmpty() && currentTime - lastUpdated < twentyFourHours) {
        return savedCookie
    }

    val homePageDocument = app.get("${mainUrl}/mobile/home", timeout = 10000L).document
    val addHash = homePageDocument.select("body").attr("data-addhash")
    val time = homePageDocument.select("body").attr("data-time")

    var verificationUrl = "https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/NF.json"
    verificationUrl = app.get(verificationUrl).parsed<VerifyUrl>().url.replace("###", addHash)
    app.get(verificationUrl + "&t=${time}")
    delay(15000)
    var verifyCheck: String
    var verifyResponse: NiceResponse
    var tries = 0

    do {
        delay(1000)
        tries++
        val requestBody = FormBody.Builder().add("verify", addHash).build()
        verifyResponse = app.post("${mainUrl}/mobile/verify2.php", requestBody = requestBody)
        verifyCheck = verifyResponse.text
    } while (!verifyCheck.contains("\"statusup\":\"All Done\"") && tries < 12)

    val cookie = verifyResponse.cookies["t_hash_t"].orEmpty()

    CineStreamPrefs.saveCookie(context, cookie)

    return cookie
}


object CineStreamPrefs {
    private const val PREF_NAME = "cine_stream_prefs"
    private const val KEY_COOKIE = "nf_cookie"
    private const val KEY_TIMESTAMP = "nf_cookie_timestamp"

    fun saveCookie(context: Context, cookie: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COOKIE, cookie)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getCookie(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_COOKIE, "") ?: ""
    }

    fun getLastUpdate(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TIMESTAMP, 0L)
    }
}
