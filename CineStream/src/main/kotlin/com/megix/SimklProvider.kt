package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.syncproviders.providers.SimklApi.Companion.getPosterUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.runAllAsync
import com.megix.CineStreamExtractors.invokeAnimeparadise
import com.megix.CineStreamExtractors.invokeGojo
import com.megix.CineStreamExtractors.invokeSudatchi
import com.megix.CineStreamExtractors.invokeAnimes
import com.megix.CineStreamExtractors.invokeAnizone
import com.megix.CineStreamExtractors.invokeTorrentio
import com.megix.CineStreamExtractors.invokeAllanime

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
        val simklId = url.substringAfterLast("/")
        val jsonString = app.get("$apiUrl/tv/$simklId?extended=full&client_id=$auth").text
        val json = parseJson<SimklResponse>(jsonString)
        val genres = json.genres?.map { it.toString() }
        val country = json.country
        val tvType = json.type.toString()

        if (tvType == "movie" || (tvType == "anime" && json.anime_type?.equals("movie") == true)) {
            val data = LoadLinksData(
                json.title,
                json.en_title,
                tvType,
                simklId?.toIntOrNull(),
                json.ids?.imdb,
                json.ids?.tmdb?.toIntOrNull(),
                json.year,
                json.ids?.anilist?.toIntOrNull(),
                json.ids?.mal?.toIntOrNull(),
                null,
                null,
                null,
            ).toJson()
            return newMovieLoadResponse("${json.en_title ?: json.title}", url, TvType.Movie, data) {
                this.posterUrl = getPosterUrl(json.poster.toString())
                this.backgroundPosterUrl = "https://simkl.in/fanart/${json.fanart}_medium.webp"
                this.plot = json.overview
                this.tags = genres
                this.rating = json.ratings?.simkl?.rating.toString().toRatingInt()
                this.year = json.year
                this.addSimklId(simklId.toInt())
                this.addAniListId(json.ids?.anilist?.toIntOrNull())
            }
        } else {
            val epsJson = app.get("$apiUrl/tv/episodes/$simklId?client_id=$auth").text
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
                        json.ids?.anilist?.toIntOrNull(),
                        json.ids?.mal?.toIntOrNull(),
                        it.season,
                        it.episode,
                        json.season?.toIntOrNull(),
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
                this.backgroundPosterUrl = "https://simkl.in/fanart/${json.fanart}_medium.webp"
                this.plot = json.overview
                this.tags = genres
                this.rating = json.ratings?.simkl?.rating.toString().toRatingInt()
                this.year = json.year
                this.addSimklId(simklId.toInt())
                this.addAniListId(json.ids?.anilist?.toIntOrNull())
            }
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = parseJson<LoadLinksData>(data)
        if(res.tvtype?.equals("anime") == true) {
            val imdbSeason = if(res.episode != null && res.imdbSeason == null) 1 else res.imdbSeason
            runAllAsync(
                { invokeAnimes(res.malId, res.anilistId, res.episode, res.year, "kitsu", subtitleCallback, callback) },
                { invokeSudatchi(res.anilistId, res.episode, subtitleCallback, callback) },
                { invokeGojo(res.anilistId, res.episode, callback) },
                { invokeAnimeparadise(res.title, res.malId, res.episode, subtitleCallback, callback) },
                { invokeAllanime(res.title, res.year, res.episode, subtitleCallback, callback) },
                { invokeAnizone(res.title, res.episode, subtitleCallback, callback) },
                { invokeTorrentio(res.imdbId, res.imdbSeason, res.episode, callback) },
            )
        }
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
        val anilistId: Int? = null,
        val malId: Int? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val imdbSeason: Int? = null,
    )
}
