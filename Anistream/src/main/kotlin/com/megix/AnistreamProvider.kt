package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId

open class AnistreamProvider : MainAPI() {
    override var mainUrl = "https://api.jikan.moe/v4"
    override var name = "Anistream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    
    override val supportedTypes = setOf(
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/top/anime" to "Top Anime",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val json = app.get(request.data).text

        val animes = parseJson<AnimeResponse>(json)
        val home = animes.data.mapNotNull { anime ->
            newMovieSearchResponse(anime.title, PassData(anime.mal_id).toJson(), TvType.Movie) {
                this.posterUrl = anime.images.jpg.image_url
            }
        }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home
            ),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val animeJson = app.get("$mainUrl/anime?q=$query&sfw").text
        val animes = parseJson<AnimeResponse>(animeJson)
        animes.data.mapNotNull { anime ->
            searchResponse.add(newMovieSearchResponse(anime.title, PassData(anime.mal_id).toJson(), TvType.Movie) {
                this.posterUrl = anime.images.jpg.image_url
            })
        }

        return searchResponse

    }


    override suspend fun load(url: String): LoadResponse? {
        val anime = parseJson<PassData>(url)
        val mal_id = anime.mal_id
        val json = app.get("$mainUrl/anime/$mal_id").text
        val animeData = parseJson<AniResponse>(json).data
        val title = animeData.title
        val posterUrl = animeData.images.jpg.image_url
        val rating = animeData.score
        val year = animeData.year
        val description = animeData.synopsis
        val url = animeData.url
        val type = animeData.type

        if(type == "TV") {
            val episodes = mutableListOf<Episode>()
            for(i in 1..animeData.episodes) {
                val episode = i
                episodes.add(
                    newEpisode(
                        LoadLinksData(
                            title,
                            mal_id,
                            year,
                            episode,
                        ).toJson()
                    ) {
                        this.episode = episode
                    }
                )
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                addMalId(mal_id)
                addAniListId(mal_id)
            }
        }
        else {
            val data = LoadLinksData(
                title,
                mal_id,
                year,
                null
            ).toJson()
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.year = year
                addMalId(mal_id)
                addAniListId(mal_id)
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
        val mal_id: Int
    )

    data class LoadLinksData(
        val title: String,
        val mal_id: Int,
        val year: Int,
        val episode: Int?,
    )

    data class AnimeResponse(
        val data: List<AnimeData>
    )

    data class AniResponse(
        val data : AnimeData
    )

    data class AnimeData(
        val mal_id: Int,
        val url: String,
        val images: Images,
        val title: String,
        val type: String,
        val status: String,
        val airing: Boolean,
        //val duration: String,
        val score: Double,
        val synopsis: String,
        val year: Int,
        val  episodes: Int,
        //val genres: List<Genre>,
    )

    data class Images(
        val jpg: Image,
        val webp: Image
    )

    data class Image(
        val image_url: String,
        val small_image_url: String,
        val large_image_url: String
    )

    // data class Genre(
    //     val mal_id: Int,
    //     val type: String,
    //     val name: String,
    //     val url: String
    // )
}