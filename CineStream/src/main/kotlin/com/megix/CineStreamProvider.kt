package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/feed.json" to "Home"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val json = app.get(request.data).text
        val movies = AppUtils.parseJson<ArrayList<Home>>(json)
        val home = movies.mapNotNull { movie ->
            newMovieSearchResponse(movie.name, PassData(movie.id, movie.type).toJson(), TvType.Movie) {
                this.posterUrl = movie.poster.toString()
            }
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        // val movieJson = app.get("$cinemeta_url/catalog/movie/top/search=$query.json").text
        // val movies = gson.fromJson(movieJson, SearchResult::class.java)
        // movies.metas.forEach {
        //     val jsonData = PassData(it.id, it.type)
        //     val data =  Json.encodeToString(jsonData)
        //     searchResponse.add(newMovieSearchResponse(it.name, data, TvType.Movie) {
        //         this.posterUrl = it.poster
        //     })
        // }

        // val seriesJson = app.get("$cinemeta_url/catalog/series/top/search=$query.json").text
        // val series = gson.fromJson(seriesJson, SearchResult::class.java)
        // series.metas.forEach {
        //     val jsonData = PassData(it.id, it.type)
        //     val data =  Json.encodeToString(jsonData)
        //     searchResponse.add(newMovieSearchResponse(it.name, data, TvType.Movie) {
        //         this.posterUrl = it.poster
        //     })
        // }

        return searchResponse
    }


    override suspend fun load(url: String): LoadResponse? {
        val movie = parseJson<PassData>(url)
        val tvtype = movie.type
        val id = movie.id
        val json = app.get("$cinemeta_url/meta/$tvtype/$id.json").text
        val movieData = AppUtils.parseJson<ResponseData>(json)

        val title = movieData.meta?.name.toString()
        val posterUrl = movieData.meta?.poster.toString()
        val imdbRating = movieData.meta?.imdbRating
        val year = movieData.meta?.year.toString()
        var description = movieData.meta?.description.toString()
        //val cast = movieData.meta?.cast
        //val genre = movieData.meta?.genre
        val background = movieData.meta?.background.toString()


        return newMovieLoadResponse(title, movie.id, TvType.Movie, movie.id) {
            this.posterUrl = posterUrl
            this.plot = description
            //this.tags = genre
            this.rating = imdbRating.toRatingInt()
            this.year = year.toIntOrNull()
            this.backgroundPosterUrl = background
            //addActors(cast)
            //addImdbUrl(imdbUrl)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        //loadExtractor(data, subtitleCallback, callback)
        // val sources = parseJson<ArrayList<EpisodeLink>>(data)
        // sources.amap {
        //     val source = it.source
        //     val link = bypass(source).toString()
        //     loadExtractor(link, subtitleCallback, callback)
        // }
        return true
    }

    data class Meta(
        val id: String?,
        val imdb_id: String?,
        val type: String?,
        val poster: String?,
        val logo: String?,
        val background: String?,
        val moviedb_id: Int?,
        val name: String?,
        val description: String?,
        val genre: List<String>?,
        val releaseInfo: String?,
        val status: String?,
        val runtime: String?,
        val cast: List<String>?,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val slug: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?
    )

    // data class SearchResult(
    //     val query: String,
    //     val rank: Double,
    //     val cacheMaxAge: Long,
    //     val metas: List<Media>
    // )

    // data class Media(
    //     val id: String,
    //     val imdb_id: String?,
    //     val type: String,
    //     val name: String,
    //     val releaseInfo: String?,
    //     val poster: String,
    //     val slug: String,
    // )

    // data class SearchMeta(
    //     val id: String,
    //     val name: String,
    //     val poster: String,
    //     val type : String,
    // )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int?,
        val episode: Int?,
        val released: String?,
        val overview: String?,
        val thumbnail: String?,
        val moviedb_id: Int?
    )

    data class ResponseData(
        val meta: Meta?,
    )

    data class Home(
        val id: String,
        val name: String,
        val releaseInfo: String?,
        val type: String,
        val poster: String?,
        val imdbRating: String?,
        val popularity: Int?
    )

    // data class EpisodeLink(
    //     val source: String
    // )

    data class PassData(
        val id: String,
        val type: String
    )
}

