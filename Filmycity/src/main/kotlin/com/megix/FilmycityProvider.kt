package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class FilmycityProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://filmycity.blog"
    override var name = "Filmycity"
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
        val home = document.select("content.movie-data").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img").attr("title").replace("Download ", "")
        val href = this.selectFirst("a").attr("href")
        val posterUrl = this.selectFirst("img").attr("src")
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?phrase=$query").document

            val results = document.select("content.movie-data").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("title").text().replace("Download ", "")
        val plot = document.selectFirst("div > p").text()
        val posterUrl = document.selectFirst("img.img-cover").attr("src")

        return newMovieLoadResponse(title, url, TvType.TvMovie, url) {
            this.posterUrl = posterUrl
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val buttons = app.get("a:contains(Direct Download|Download links)")

        buttons.mapNotNull {
            val href = it.attr("href")
            val res = app.get(href)
            val cookies = res.cookies["PHPSESSID"]
            val doc = res.document
            val token = doc.select("input[name=token]").attr("value")

            val response = app.post(
                href,
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                    "Content-Type" to "application/x-www-form-urlencoded",
                    "Referer" to "$href"
                ),
                data = mapOf(
                    "token" to "$token",
                ),
                cookies = cookies
            ).toString()

            callback.invoke(
                ExtractorLink(
                    "Filmycity",
                    "Filmycity",
                    response,
                    referer = "",
                    Qualities.Unknown.Value
                )
            )
        }

        return true   
    }
}
