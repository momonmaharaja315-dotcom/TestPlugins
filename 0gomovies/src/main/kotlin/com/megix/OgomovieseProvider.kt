package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.json.JSONObject

open class OgomoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://0gomovies.movie/ogomovies"
    override var name = "0gomovies"
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
        val home = document.select("div.ml-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a > img").attr("alt") ?: ""
        val href = this.selectFirst("a").attr("href") ?: ""
        val posterUrl = this.selectFirst("a > img").attr("src") ?: ""
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.ml-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.detail-mod > h3") ?. text() ?: ""
        val posterUrl = document.selectFirst("meta[property=og:image]") ?. attr("content") ?: document.selectFirst("div.sheader noscript img") ?. attr("src")
     
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
        var headers = mapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        val document = app.get("${data}/watching/", headers = headers).document
        val link = document.selectFirst("li.episode-item").attr("data-drive").toString()
<<<<<<< HEAD
        loadExtractor(data, subtitleCallback, callback)
=======
        val doc = app.get(link).document
        val scriptTag = doc.selectFirst("script:containsData(playerjsSubtitle)").toString()
        val matchResult = Regex("""FirePlayer\|([^|]*)\|""").find(scriptTag)
        val dataId = matchResult ?. groups ?. get(1) ?. value
        headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val response = app.post(
            "https://cdn.asumanaksoy.com/player/index.php?data=${dataId}&do=getVideo", 
            headers = headers
        ).text 
        val jsonObject = JSONObject(response)

        val securedLink = jsonObject.getString("securedLink")
        
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                securedLink,
                referer = "",
                quality = Qualities.Unknown.value
            )
        )
        //loadExtractor(data, subtitleCallback, callback)
>>>>>>> 90e3987be0761ce7dcfc479e7b8acfc967488738
        return true   
    }
}