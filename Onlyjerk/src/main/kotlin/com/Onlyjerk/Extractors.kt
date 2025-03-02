package com.megix

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.DoodLaExtractor

class Dooodster : DoodLaExtractor() {
    override var mainUrl = "https://dooodster.com"
}

class Bigwarp : ExtractorApi() {
    override var name = "Bigwarp"
    override var mainUrl = "https://bigwarp.io"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = app.get(url, allowRedirects = false).headers["location"] ?: url
        val source = app.get(link).document.selectFirst("body > script").toString()
        val regex = """https?:\/\/[^\s]+\.mp4[^\s]*"""
        val pattern = Regex(regex)
        val match = pattern.find(text)

        if (match != null) {
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    match.value,
                    "",
                    Qualities.Unknown.value
                )
            )
        }
    }
}
