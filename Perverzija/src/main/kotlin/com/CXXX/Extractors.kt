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
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, , subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit){
        // val response = app.get(
        //     url, referer = "https://${url.substringAfter("//").substringBefore("/")}/",
        // )
        val response = app.get(
            url, referer = referer,
        )

        callback.invoke(
            ExtractorLink(
                name,
                name,
                response.document.toString(),
                "",
                Qualities.Unknown.value
            )
        )


        val playerScript =
            response.document.selectXpath("//script[contains(text(),'var video_id')]")
                .html()

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
                    callback.invoke(
                        ExtractorLink(
                            name,
                            name,
                            link,
                            "",
                            Qualities.Unknown.value
                        )
                    )
                }
            }
        }

    }
}
