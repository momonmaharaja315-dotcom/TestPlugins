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
        val title = this.selectFirst("a").attr("alt")
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
        val title = document.selectFirst("title").text().replace("Watch ", "")
        var poster = document.selectFirst("img.film-poster-img").attr("src")
        val url = fixUrl(document.selectFirst("a.btn-play").attr("href"))
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val qualities = document.select("div#servers-content")
        qualities.mapNotNull {
            val id = it.selectFirst("div.item").attr("data-id")
            val url = "https://deaddrive.xyz/embed/$id"
            val doc = app.get(url).document
            val sources = doc.select("ul.list-server-items").select("li")
            sources.mapNotNull { source ->
                callback.invoke(
                    ExtractorLink(
                        "Deadstream",
                        "Deadstream",
                        source.attr("data-video"),
                        referer = "",
                        quality = Qualities.Unknown.value
                    )
                )
            }
        }
        return true
    }
}
