package com.CXXX

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class Xtremestream : ExtractorApi() {
    override var name = "Xtremestream"
    override var mainUrl = "https://perv.xtremestream.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit){

        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Accept-Language" to "en-GB,en;q=0.8",
            "Priority" to "u=0, i",
            "Referer" to "$referer",
            "Sec-Ch-Ua" to "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Brave\";v=\"126\"",
            "Sec-Ch-Ua-Mobile" to "?0",
            "Sec-Ch-Ua-Platform" to "\"Linux\"",
            "Sec-Fetch-Dest" to "iframe",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "cross-site",
            "Sec-Gpc" to "1",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        )

        val response = app.get(url, referer = referer, headers = headers)

        callback.invoke(
            ExtractorLink(
                "response",
                "response",
                response.document.toString(),
                "",
                Qualities.Unknown.value,
            )
        )

        val playerScript =
            response.document.selectXpath("//script[contains(text(),'var video_id')]")
                .html()
        val sources = mutableListOf<ExtractorLink>()
        if (playerScript.isNotBlank()) {
            val videoId = playerScript.substringAfter("var video_id = `").substringBefore("`;")
            val m3u8LoaderUrl =
                playerScript.substringAfter("var m3u8_loader_url = `").substringBefore("`;")

            if (videoId.isNotBlank() && m3u8LoaderUrl.isNotBlank()) {
                M3u8Helper.generateM3u8(
                    name,
                    "$m3u8LoaderUrl/$videoId",
                    "$m3u8LoaderUrl/$videoId"
                ).forEach { link ->
                    sources.add(link)
                }
            }
        }
        sources.forEach { source ->
            callback.invoke(source)
        }

    }
}
