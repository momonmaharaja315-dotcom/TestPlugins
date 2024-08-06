package com.Anitime
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import org.json.JSONObject

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val qPrefix = "whw"
        val document = app.get(url).document
        
        val responseText = document.toString()
        val base64Pattern = Regex("PLAYER\\(atob\\(\"(.*?)\"\\)")
        val base64Value = base64Pattern.find(responseText)?.groups?.get(1)?.value ?: ""
        val decodedJson = base64Decode(base64Value)
        val jsonObject = JSONObject(decodedJson)

        val domain = jsonObject.getString("domain")
        val vidId = jsonObject.getString("id")
        val sources = jsonObject.getJSONArray("sources")
        val link = "https://$domain/$qPrefix$vidId"
        val headers = mapOf(
            "Referer" to "$mainUrl",
            "Sec-Fetch-Mode" to "cors"
        )

        callback.invoke (
            ExtractorLink (
                this.name,
                this.name,
                link,
                referer = mainUrl,
                Qualities.Unknown.value,
                headers = headers
            )
        )
    }
}