package com.megix

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.Filesim

class WIMP : MainAPI() {
    override var mainUrl              = "https://whereismyporn.com"
    override var name                 = "WhereIsMyPorn"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "" to "Latest",
        "/archives/category/brazzers" to "Brazzers",
        "/archives/category/deeper" to "Deeper",
        "/archives/category/pornfidelity" to "PornFidelity",
        "/archives/category/vixen" to "Vixen",
        "/archives/category/onlyfans" to "OnlyFans",
        "/archives/category/bangbus" to "BangBus",
        "/archives/category/blacked" to "Blacked",
        "/archives/category/blackedraw" to "BlackedRaw",
        "/archives/category/porn-movie" to "Porn Movies",
        "/archives/category/naughtyamerica" to "NaughtyAmerica",
        "/archives/category/digitalplayground" to "DigitalPlayground",
        "/archives/category/familystrokes" to "FamilyStrokes",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl${request.data}/page/$page/").document
        val home     = document.select("article.post").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
                list    = HomePageList(
                name    = request.name,
                list    = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = this.select("h2").text()
        val href      = this.select("a").attr("href")
        val posterUrl = this.select("a > img").attr("src")

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..10) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document
            val results = document.select("article.post").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title       = document.select("title").text()
        val link        = document.select("div.entry-content iframe").attr("src")
        val poster      = app.get(link).document.select("div.vplayer > img").attr("src")

        return newMovieLoadResponse(title, url, TvType.NSFW, link) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        loadExtractor(
            data,
            "",
            subtitleCallback,
            callback
        )
        return true
    }
}

class Doodporn: StreamWishExtractor() {
    override var name = "Doodporn"
    override var mainUrl = "https://doodporn.xyz"
}

class Filemoonlink: Filesim() {
    override var mainUrl = "https://filemoon.link"
}
