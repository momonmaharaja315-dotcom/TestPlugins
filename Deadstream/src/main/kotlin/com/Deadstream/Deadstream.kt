package com.Deadstream

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Deadstream : MainAPI() {
    override var mainUrl = "https://deadstream.xyz"
    override var name = "Deadstream"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie,TvType.Anime)

    override val mainPage = mainPageOf(
        "$mainUrl/recently-updated" to "Latest",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + "?page=$page").document
        val home = document.select("div.flw-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse (
            list = HomePageList (
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = false
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.selectFirst("a").attr("title")
        val href = fixUrl(this.selectFirst("a").attr("href"))
        val posterUrl = fixUrl(this.selectFirst("img").attr("data-src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val document = app.get("${mainUrl}/search?keyword=${query}").document
        val results = document.select("div.flw-item").mapNotNull { it.toSearchResult() }
        searchResponse.addAll(results)
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h2.film-name").text()
        val div = document.selectFirst("div[style*=background-image]")
        val posterUrl = div.attr("style").substringAfter("url(").substringBefore(")")
        val url = fixUrl(document.selectFirst("a.btn-play").attr("href"))
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val quality = document.selectFirst("div#servers-content")

        quality.select("div.item").mapNotNull {
            val id = it.attr("data-embed")
            val url = "https://deaddrive.xyz/embed/$id"
            val doc = app.get(url).document
            doc.select("ul.list-server-items").select("li").mapNotNull { source ->
                loadExtractor(source.attr("data-video"), subtitleCallback, callback)
            }
        }
        return true
    }
}
