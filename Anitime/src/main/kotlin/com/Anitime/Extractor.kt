package com.Anitime
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.util.Base64
import org.json.JSONObject
import java.util.regex.Pattern

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val qPrefix = "whw"
        val document = app.get(url).document
        val responseText = document.toString()

        val base64Pattern = Pattern.compile("""PLAYER\(atob\(\"(.*?)\"\)""")
        val matcher = base64Pattern.matcher(responseText)
        val decodedJson = String(Base64.getDecoder().decode(matcher.group(1)))
        val jsonObject = JSONObject(decodedJson)

        val domain = jsonObject.getString("domain")
        val vidId = jsonObject.getString("id")
        val sources = jsonObject.getJSONArray("sources")
        val link = "https://$domain/$qPrefix$vidId"
        callback.invoke (
            ExtractorLink (
                this.name,
                this.name,
                link,
                referer = url,
                Qualities.Unknown.value
            )
        )
    }
}