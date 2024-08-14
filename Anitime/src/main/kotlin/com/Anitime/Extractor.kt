package com.Anitime
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.Request

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = true
    data class Source(
        val label: String,
        val type: String
    )

    data class ResponseData(
        val sources: List<Source>,
        val id: String,
        val domain: String,
    )

    private val client = OkHttpClient()

    constructor() {
        this.client = OkHttpClient()
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url, referer = mainUrl).text
        val regex = Regex("(ﾟωﾟﾉ=.+?) \\('_'\\);")
        val match = regex.find(doc)
        val data2 = match?.groupValues?.get(1) ?: ""

        val requestBody = FormBody.Builder()
            .add("abyss", data2)
            .build()
        val request = Request.Builder()
            .url("https://abyss-oybwdysyx-saurabhkaperwans-projects.vercel.app/decode")
            .post(requestBody)
            .build()
        val response = this.client.newCall(request).execute()
        val jsonDataString = response.body?.string() ?: ""
        val responseData = Gson().fromJson(jsonDataString, ResponseData::class.java)

        responseData.sources.forEach { source: Source ->
            val label = source.label
            val domain = "https://${responseData.domain}"
            val id = responseData.id
            var url = ""
            when (label) {
                "360p" -> url = "$domain/$id"
                "720p" -> url = "$domain/www$id"
                "1080p" -> url = "$domain/whw$id"
            }

            val headers = mapOf(
                "Sec-Fetch-Mode" to "cors",
            )

            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    url,
                    referer = mainUrl,
                    getQualityFromName(label),
                    headers = headers
                )
            )
        }
    }
}
