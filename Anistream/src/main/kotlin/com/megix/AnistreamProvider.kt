package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId

open class AnistreamProvider : MainAPI() {
    override var mainUrl = BuildConfig.CONSUMET_API
    override var name = "Anistream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    
    override val supportedTypes = setOf(
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "meta/anilist/latest" to "Latest Anime",
        "meta/anilist/trending" to "Trending Anime",
        "meta/anilist/popular" to "Popular Anime",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val json = app.get("${mainUrl}/${request.data}?page=${page}").text
        val animes = parseJson<AnistreamResponse>(json)
        
        val home = animes.results.mapNotNull { result ->
            newMovieSearchResponse(result.title.romaji, PassData(result.id).toJson(), TvType.Movie) {
                this.posterUrl = result.image
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home
            ),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val json = app.get("$mainUrl/meta/anilist/${query}").text
        val animes = parseJson<AnistreamResponse>(json)
        animes.results.mapNotNull { result ->
            searchResponse.add(newMovieSearchResponse(result.title.romaji, PassData(result.id).toJson(), TvType.Movie) {
                this.posterUrl = result.image
            })
        }
        return searchResponse
    }


    override suspend fun load(url: String): LoadResponse? {
        val anime = parseJson<PassData>(url)
        val id = anime.id
        val json = app.get("$mainUrl/meta/anilist/info/${id}").text
        val animeData = parseJson<Results>(json)
        val title = animeData.title.romaji
        val posterUrl = animeData.image
        val rating = animeData.rating
        val year = animeData.releaseDate
        val description = animeData.description
        val type = animeData.type
        if(type == "TV") {
            val episode = animeData.episodes.map {
                newEpisode(
                    LoadLinksData(
                        it.id,
                        title,
                        id,
                        year,
                        type,
                    ).toJson()
                ) {
                    this.name = it.title
                    this.episode = it.number
                    this.posterUrl = it.image
                }
            } ?: emptyList()

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.rating = rating
                this.year = year
                this.backgroundPosterUrl = animeData.cover
                addAniListId(id.toInt())
            }
        }
        else {
            val epId = animeData.episodes[0].id
            val data = LoadLinksData(
                epId,
                title,
                id,
                year,
                type,
            ).toJson()

            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.rating = rating
                this.year = year
                this.backgroundPosterUrl = animeData.cover
                addAniListId(id.toInt())
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
        val json = app.get("$mainUrl/meta/anilist/watch/${res.epId}").text
        val animeData = parseJson<GetSources>(json)
        val referer = animeData.headers.Referer
        animeData.sources.map {
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    it.url,
                    referer,
                    getQualityFromName(it.quality),
                    it.isM3U8 ?: false
                )
            )
        }
        // argamap(
        //     {
        //         invokeMoviesdrive(
        //             res.title,
        //             res.season,
        //             res.episode,
        //             year,
        //             subtitleCallback,
        //             callback
        //         )
        //     },
        // )
        return true
    }

    data class PassData(
        val id: String
    )

    data class LoadLinksData(
        val epId: String,
        val title: String,
        val id: String,
        val year: Int,
        val type: String,
    )

    data class AnistreamResponse (
        var currentPage : Int,
        var hasNextPage : Boolean,
        var results     : ArrayList<Results> = arrayListOf()
    )
    data class Results (
        var id            : String,
        var malId         : Int,
        var title         : Title = Title(),
        var image         : String,
        var description   : String,
        var status        : String,
        var cover         : String,
        var rating        : Int,
        var releaseDate   : Int,
        var color         : String,
        var genres        : ArrayList<String> = arrayListOf(),
        var totalEpisodes : Int,
        var duration      : Int,
        var type          : String,
        var episodes      : ArrayList<Episodes> = arrayListOf()
    )

    data class Episodes (
        var id          : String,
        var title       : String,
        var description : String? = null,
        var number      : Int,
        var image       : String? = null,
    )

    data class Title (
        var romaji        : String = "",
        var english       : String = "",
        var native        : String = "",
    )

    data class GetSources (
        var headers  : Headers           = Headers(),
        var sources  : ArrayList<Sources> = arrayListOf(),
        var download : String,
    )

    data class Headers (
        var Referer : String = ""
    )

    data class Sources (
        var url     : String,
        var isM3U8  : Boolean,
        var quality : String
    )

}
