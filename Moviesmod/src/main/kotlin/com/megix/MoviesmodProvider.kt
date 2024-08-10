package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode
import java.net.URI
import org.jsoup.nodes.Document

class MoviesmodProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesmod.band"
    override var name = "Moviesmod"
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

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a").attr("title")
        val href = this.selectFirst("a").attr("href")
        val posterUrl = this.selectFirst("a > div > img").attr("src")
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search/$query/page/$i").document

            val results = document.select("div.post-cards > article").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    fun fixUrl(url: String, domain: String): String {
        if (url.startsWith("http")) {
            return url
        }
        if (url.isEmpty()) {
            return ""
        }

        val startsWithNoHttp = url.startsWith("//")
        if (startsWithNoHttp) {
            return "https:$url"
        } else {
            if (url.startsWith('/')) {
                return domain + url
            }
            return "$domain/$url"
        }
    }

    fun getBaseUrl(url: String): String {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    }

    suspend fun bypass(url: String): String? {
        fun Document.getFormUrl(): String {
            return this.select("form#landing").attr("action")
        }

        fun Document.getFormData(): Map<String, String> {
            return this.select("form#landing input").associate { it.attr("name") to it.attr("value") }
        }

        val host = getBaseUrl(url)
        var res = app.get(url).document
        var formUrl = res.getFormUrl()
        var formData = res.getFormData()

        res = app.post(formUrl, data = formData).document
        formUrl = res.getFormUrl()
        formData = res.getFormData()

        res = app.post(formUrl, data = formData).document
        val skToken = res.selectFirst("script:containsData(?go=)")?.data()?.substringAfter("?go=")
            ?.substringBefore("\"") ?: return null
        val driveUrl = app.get(
            "$host?go=$skToken", cookies = mapOf(
                skToken to "${formData["_wp_http2"]}"
            )
        ).document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
        val path = app.get(driveUrl ?: return null).text.substringAfter("replace(\"")
            .substringBefore("\")")
        if (path == "/404") return null
        return fixUrl(path, getBaseUrl(driveUrl))
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]").attr("content").replace("Download ", "")
        val posterUrl = document.selectFirst("meta[property=og:image]").attr("content")
        val description = document.selectFirst("div.imdbwp__teaser").text()

        //val tvSeriesEpisodes = mutableListOf<Episode>()
        //var seasonNum = 1
        //val seasonList = mutableListOf<Pair<String, Int>>()
        //val buttons = document.select("a.maxbutton-episode-links,.maxbutton-g-drive,.maxbutton-af-download")

        // buttons.mapNotNull {
        //     var link = it.attr("href")

        //     if(link.contains("url=")) {
        //         val base64Value = link.substringAfter("url=")
        //         link = base64Decode(base64Value)
        //     }

        //     val doc = app.get(link).document
        //     val hTags = doc.select("h3,h4")

        //     hTags.mapNotNull {
        //         val title = it.text()
        //         var epUrl = it.selectFirst("a").attr("href")
        //         if(epUrl.contains("unblockedgames")) {
        //             epUrl = bypass(epUrl)
        //         }
        //         tvSeriesEpisodes.add (
        //             newEpisode(epUrl) {
        //                 name = title
        //                 season = seasonNum
        //             }
        //         )
        //     }
        //     seasonNum++
        // }
        // return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
        //     this.posterUrl = posterUrl
        //     //this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
        //     this.plot = description
        // }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("a.maxbutton-download-links").mapNotNull {
            var link = it.attr("href")
            if(link.contains("url=")) {
                 val base64Value = link.substringAfter("url=")
                 link = base64Decode(base64Value)
            }

            val doc = app.get(link).document
            val url = doc.selectFirst("a.maxbutton-1").attr("href")
            val source = bypass(url).toString()

            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    source,
                    referer = "",
                    Qualities.Unknown.value
                )
            )
        }
        //loadExtractor(data, subtitleCallback, callback)

        return true
    }
}
