package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId

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
            newMovieSearchResponse(it.title, "$mainUrl/${it.ids?.simkl_id}") {
                this.posterUrl = getPosterUrl(it.poster.toString())
            }
        }
        return data
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val jsonString = app.get(apiUrl + request.data + page).text
        val json = parseJson<Array<SimklResponse>>(jsonString)
        val data = json.map {
            newMovieSearchResponse("it.title", "$mainUrl/${it.ids?.simkl_id}") {
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
        val simklId = url.substringAfterLast("/")
        val jsonString = app.get("$apiUrl/movies/$simklId?extended=full&client_id=$auth").text
        val json = parseJson<SimklResponse>(jsonString)
        val title = json.title
        val poster = json.poster
        val type = json.type
        val year = json.year
        val overview = json.overview
        val genres = json.genres?.map { it.toString() }
        val country = json.country
        val imdbId = json.ids?.imdb
        val tmdbId = json.ids?.tmdb
        //val recommendations = json.users_recommendations?.map { it.toSearchResponse() }
        return newMovieLoadResponse(json.title, url, TvType.Movie, url) {
            this.posterUrl = json.poster
            this.plot = json.overview
            this.tags = genres
            this.rating = json.ratings?.simkl?.rating?.toInt()
            this.year = json.year
            this.addSimklId(simklId.toInt())
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
        var title          : String,
        var year           : Int?     = null,
        var type           : String?  = null,
        var url            : String?  = null,
        var poster         : String?  = null,
        var fanart         : String?  = null,
        var ids            : Ids?     = Ids(),
        var release_date   : String?  = null,
        var ratings        : Ratings = Ratings(),
        var country        : String?  = null,
        var certification  : String?  = null,
        var runtime        : String?  = null,
        var status         : String?  = null,
        var total_episodes : Int?     = null,
        var network        : String?  = null,
        var overview       : String?  = null,
        var genres         : ArrayList<String> = arrayListOf(),
        var users_recommendations : ArrayList<UsersRecommendations> = arrayListOf()
    )

    data class Ids (
        var simkl_id : Int?    = null,
        var tmdb     : String? = null,
        var imdb     : String? = null,
        var slug     : String? = null
    )

    data class Ratings (
        var simkl : Simkl? = Simkl(),
    )

    data class Simkl (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class UsersRecommendations (
        var title  : String,
        var year   : Int?    = null,
        var poster : String? = null,
        var type   : String? = null,
        var ids    : Ids   = Ids()
    )
}
