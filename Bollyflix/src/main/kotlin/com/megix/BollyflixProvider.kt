package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode

class BollyflixProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://bollyflix.wales"
    override var name = "BollyFlix"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.post-cards > article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private suspend fun bypass(url: String): String {
        val document = app.get(url).text
        val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
        return base64Decode(encodeUrl)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a") ?. attr("title") ?. replace("Download ", "").toString()
        val href = this.selectFirst("a") ?. attr("href").toString()
        val posterUrl = this.selectFirst("img") ?. attr("src").toString()
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search/$query/page/$i/").document

            val results = document.select("div.post-cards > article").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()?.replace("Download ", "").toString()
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()

    
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val id = document.selectFirst("a.dl")?.attr("src")?.substringAfterLast("id=")
        val decodeUrl = bypass("https://web.sidexfee.com/?id=$id")
        //val gdflixUrl = app.get(decodeUrl, allowRedirects = false).headers["location"].toString()
        callback.invoke(
            ExtractorLink(
                name,
                name,
                decodeUrl,
                "",
                Qualities.Unknown.value
            )
        )
        return true
    }
}
