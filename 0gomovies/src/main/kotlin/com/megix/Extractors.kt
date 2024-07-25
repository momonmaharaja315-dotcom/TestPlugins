package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class Asumanaksoy: ExtractorApi() {
    override val name: String = "Asumanaksoy"
    override val mainUrl: String = "https://cdn.asumanaksoy.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(link).document
        val scriptTag = doc.selectFirst("script:containsData(playerjsSubtitle)").toString()
        val matchResult = Regex("""FirePlayer\|([^|]*)\|""").find(scriptTag)
        val dataId = matchResult ?. groups ?. get(1) ?. value
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val response = app.post(
            "https://cdn.asumanaksoy.com/player/index.php?data=${dataId}&do=getVideo", 
            headers = headers
        ).text 
        val jsonObject = JSONObject(response)

        val securedLink = jsonObject.getString("securedLink")
        
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                securedLink,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = true,
            )
        )
    }
}