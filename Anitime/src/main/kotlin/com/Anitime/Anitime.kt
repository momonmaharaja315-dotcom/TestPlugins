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
        var poster = document.selectFirst("img.rounded-sm").attr("src").toString()
        val allUrl = fixUrl(document.selectFirst("div.flex > a.flex").attr("href").toString())
        val doc = app.get(allUrl).document
        val tvSeriesEpisodes = mutableListOf<Episode>()
        var seasonNum = 1
        val seasonList = mutableListOf<Pair<String, Int>>()
        var i = 1
        doc.select("div.item").mapNotNull {
            val link = fixUrl(it.selectFirst("a").attr("href").toString())
            val text = it.text() ?: ""
            seasonList.add("$text" to seasonNum)
            val doc1 = app.get(link).document
            doc1.select("div#episodes-content button").mapNotNull {
                val onclickValue = it.attr("onclick")
                val epText = it.attr("id") ?: ""
                val regex = Regex("'(https?://[^']+)'")
                val matchResult = regex.find(onclickValue)
                val source = matchResult ?. groups ?. get(1) ?. value
                tvSeriesEpisodes.add(
                    newEpisode(source){
                        name = "Episode $epText"
                        season = seasonNum
                        episode = i
                    }
                )
                i++
            }
            seasonNum++
            i = 1
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = poster
                this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        if(data.contains("gogoanime")) {
            val source = document.selectFirst("media-player").attr("src").toString()
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    source,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true
                )
            )
        }
        else {
            val source = document.selectFirst("iframe").attr("src").toString()
            loadExtractor(source, subtitleCallback, callback)
        }
            // callback.invoke (
            //     ExtractorLink (
            //         this.name,
            //         text,
            //         epLink,
            //         referer = "",
            //         quality = Qualities.Unknown.value
            //     )
            // )
        return true
    }
}
