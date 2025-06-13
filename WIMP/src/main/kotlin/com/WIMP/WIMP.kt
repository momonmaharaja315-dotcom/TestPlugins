package com.megix

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

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
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl + request.data + "/page/$page").document
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
        val posterUrl = this.select("img").attr("src")

        return newMovieSearchResponse(title, PassData(href, posterUrl).toJson(), TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..7) {
            val document = app.get("$mainUrl/page/$i?s=$query").document
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
        val data = parseJson<PassData>(url)
        val document = app.get(data.url).document
        val iframe = document.select("iframe").attr("src")
        val title = document.select("title").text()

        return newMovieLoadResponse(title, data.url, TvType.NSFW, data.url) {
            this.posterUrl = data.posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        app.get(data).document.select("iframe").amap {
            loadExtractor(it.attr("src"), mainUrl, subtitleCallback, callback)
        }

        return true
    }
}

class Doodporn : StreamWishExtractor() {
    override val name = "Doodporn"
    override val mainUrl = "https://doodporn.xyz"
}

class PassData(
    val url: String,
    val posterUrl: String
)
