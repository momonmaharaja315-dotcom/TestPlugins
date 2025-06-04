package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl

class SimklProvider: MainAPI() {
    override var name = "Simkl"
    override var mainUrl = "https://simkl.com"
    override var supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Torrent
    )
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false
    private val apiUrl = "https://api.simkl.com"
    private final val mediaLimit = 20
    private val auth = BuildConfig.SIMKL_API

    override val mainPage = mainPageOf(
        "/movies/trending/month?client_id=$auth&extended=overview&limit=$mediaLimit&page=" to "Trending Movies",
        // "/tv/trending/month?type=series&client_id=$auth&extended=overview&limit=$mediaLimit&page=" to "Trending TV Shows",
    )

    override suspend fun search(query: String): List<SearchResponse>? {
        val jsonString = app.get("$apiUrl/search/movie?q=$query&client_id=$auth").text
        val json = parseJson<Array<SimklResponse>>(jsonString)
        val data = json.map {
            newMovieSearchResponse("${it.title}", "$mainUrl/${it.ids?.simkl_id}") {
                this.posterUrl = getPosterUrl(it.poster.toString())
            }
        }
        return data
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val jsonString = app.get(apiUrl + request.data + page).text
        val json = parseJson<Array<SimklResponse>>(jsonString)
        val data = json.map {
            newMovieSearchResponse("${it.title}", "$mainUrl/${it.ids?.simkl_id}") {
                this.posterUrl = getPosterUrl(it.poster.toString())
            }
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = data,
            ),
            hasNext = true
        )
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")
        return newMovieLoadResponse(id, url, TvType.Movie, url) {
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback.invoke(
            newExtractorLink(
                "test",
                "test",
                data,
            )
        )
        return true
    }

    data class SimklResponse (
        var title          : String?  = null,
        var url            : String?  = null,
        var poster         : String?  = null,
        var fanart         : String?  = null,
        var ids            : Ids?     = Ids(),
        var release_date   : String?  = null,
        var rank           : Int?     = null,
        var drop_rate      : String?  = null,
        var watched        : Int?     = null,
        var plan_to_watch  : Int?     = null,
        var ratings        : Ratings? = Ratings(),
        var country        : String?  = null,
        var runtime        : String?  = null,
        var status         : String?  = null,
        var total_episodes : Int?     = null,
        var network        : String?  = null,
        var overview       : String?  = null

    )

    data class Ids (
        var simkl_id : Int?    = null,
        var slug     : String? = null
    )

    data class Ratings (
        var simkl : Simkl? = Simkl(),
        var imdb  : Imdb?  = Imdb()
    )

    data class Simkl (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class Imdb (
        var rating : Double? = null,
        var votes  : Int?    = null
    )
}
