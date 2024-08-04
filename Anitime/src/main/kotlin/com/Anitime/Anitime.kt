package com.Anitime

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Anitime : MainAPI() {
    override var mainUrl = "https://anitime.aniwow.in"
    override var name = "Anitime"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie,TvType.Anime)

    override val mainPage = mainPageOf(
        "$mainUrl" to "Home",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data).document
        val home = document.select("div.grid > div.bg-gradient-to-t").mapNotNull { it.toSearchResult() }

        return newHomePageResponse (
            list = HomePageList (
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.attr("title")
        val href = fixUrl(this.selectFirst("a").attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img").attr("src").toString())
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }


    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("${mainUrl}/index.php/search/page/${i}/?keyword=${query}").document

            val results = document.select("div.grid > div.bg-gradient-to-t").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h2").text().toString()
        //val href = document.selectFirst("a:contains(Watch)").attr("href").toString()
        var poster = document.selectFirst("img.rounded-sm").attr("src").toString()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val url = fixUrl(document.selectFirst("div.flex > a.flex").attr("href").toString())
        val doc = app.get(url).document
        doc.select("div.item").mapNotNull {
            val link = fixUrl(it.selectFirst("a").attr("href").toString())
            val text = it.text() ?: ""
            val docs = app.get(link).document
            val source = docs.selectFirst("iframe").attr("src").toString()
            callback.invoke (
                ExtractorLink (
                    this.name,
                    text,
                    source,
                    referer = "",
                    quality = Qualities.Unknown.value
                )
            )
        }
        return true
    }
}
