package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.utils.AppUtils.toJson

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
        "/tv/trending/month?type=series&client_id=$auth&extended=overview&limit=$mediaLimit&page=" to "Trending TV Shows",
        "/anime/trending/?extended=overview,metadata,tmdb,genres,trailer&client_id=$auth&limit=$mediaLimit&page=" to "Trending Anime",
    )

    private fun extractId(url: String): Pair<String, String> {
        val regex = Regex("""simkl\.com/(tv|movies|anime)/(\d+)""")
        val match = regex.find(url)
        return if (match != null) {
            val tvType = match.groupValues[1]
            val id = match.groupValues[2]
            Pair(id, tvType)
        } else {
            Pair("", "")
        }
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val jsonString = app.get("$apiUrl/search/movie?q=$query&client_id=$auth").text
        val json = parseJson<Array<SimklResponse>>(jsonString)
        val data = json.map {
            newMovieSearchResponse("${it.title}", "$mainUrl${it.url}") {
                this.posterUrl = getPosterUrl(it.poster.toString())
            }
        }
        return data
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val jsonString = app.get(apiUrl + request.data + page).text
        val json = parseJson<Array<SimklResponse>>(jsonString)
        val data = json.map {
            newMovieSearchResponse("${it.title}", "$mainUrl${it.url}") {
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
        val (simklId, tvType) = extractId(url)
        val jsonString = app.get("$apiUrl/$tvType/$simklId?extended=full&client_id=$auth").text
        val json = parseJson<SimklResponse>(jsonString)
        val genres = json.genres?.map { it.toString() }
        val country = json.country

        if (tvType == "movies" || (tvType == "anime" && json.anime_type?.equals("movie") == true)) {
            val data = LoadLinksData(
                json.title,
                json.en_title,
                tvType,
                simklId?.toIntOrNull(),
                json.ids?.imdb,
                json.ids?.tmdb?.toIntOrNull(),
                json.year,
            ).toJson()
            return newMovieLoadResponse("${json.en_title ?: json.title}", url, TvType.Movie, data) {
                this.posterUrl = getPosterUrl(json.poster.toString())
                this.backgroundPosterUrl = getPosterUrl(json.fanart.toString())
                this.plot = json.overview
                this.tags = genres
                this.rating = json.ratings?.simkl?.rating.toString().toRatingInt()
                this.year = json.year
                this.addSimklId(simklId.toInt())
            }
        } else {
            val epsJson = app.get("$apiUrl/$tvType/episodes/$simklId?client_id=$auth").text
            val eps = parseJson<Array<Episodes>>(epsJson)
            val episodes = eps.map {
                newEpisode(
                    LoadLinksData(
                        json.title,
                        json.en_title,
                        tvType,
                        simklId?.toIntOrNull(),
                        json.ids?.imdb,
                        json.ids?.tmdb?.toIntOrNull(),
                        json.year,
                    ).toJson()
                ) {
                    this.season = it.season
                    this.episode = it.episode
                    this.posterUrl = "https://simkl.in/episodes/${it.img}_c.webp"
                    addDate(it.date)
                }
            }

            return newTvSeriesLoadResponse("${json.en_title ?: json.title}", url, TvType.TvSeries, episodes) {
                this.posterUrl = getPosterUrl(json.poster.toString())
                this.backgroundPosterUrl = getPosterUrl(json.fanart.toString())
                this.plot = json.overview
                this.tags = genres
                this.rating = json.ratings?.simkl?.rating.toString().toRatingInt()
                this.year = json.year
                this.addSimklId(simklId.toInt())
            }
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
        var en_title       : String?  = null,
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
        var anime_type     : String?  = null,
        var season         : String?  = null,
        var genres         : ArrayList<String> = arrayListOf(),
        var users_recommendations : ArrayList<UsersRecommendations> = arrayListOf()
    )

    data class Ids (
        var simkl_id : Int?    = null,
        var tmdb     : String? = null,
        var imdb     : String? = null,
        var slug     : String? = null,
        var mal      : String? = null,
        var anilist  : String? = null,
        var kitsu    : String? = null,
        var anidb    : String? = null
    )

    data class Ratings (
        var simkl : Simkl? = Simkl(),
    )

    data class Simkl (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class UsersRecommendations (
        var title  : String? = null,
        var year   : Int?    = null,
        var poster : String? = null,
        var type   : String? = null,
        var ids    : Ids   = Ids()
    )

    data class Episodes (
        var season  : Int?     = null,
        var episode : Int?     = null,
        var type    : String?  = null,
        var aired   : Boolean? = null,
        var img     : String?  = null,
        var date    : String?  = null,
    )
    data class LoadLinksData(
        val title: String? = null,
        val eng_title : String? = null,
        val tvtype: String? = null,
        val simklId: Int? = null,
        val imdbId: String? = null,
        val tmdbId: Int? = null,
        val year: Int? = null,
    )
}
