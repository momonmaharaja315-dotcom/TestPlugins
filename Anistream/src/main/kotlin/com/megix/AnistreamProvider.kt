package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

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
            newMovieSearchResponse(anime.title, PassData(anime.malId).toJson(), TvType.Movie) {
                this.posterUrl = anime.images.jpg.imageUrl
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
        val animeJson = app.get("$mainUrl/v4/anime?q=$query&sfw").text
        val animes = parseJson<AnimeResponse>(animeJson)
        animes.data.forEach { anime ->
            searchResponse.add(newMovieSearchResponse(anime.title, PassData(anime.malId).toJson(), TvType.Movie) {
                this.posterUrl = anime.images.jpg.imageUrl
            })
        }

        return searchResponse

    }


    override suspend fun load(url: String): LoadResponse? {
        val anime = parseJson<PassData>(url)
        val malId = anime.malId
        val json = app.get("$mainUrl/anime/$malId").text
        val animeData = parseJson<AnimeResponse>(json).data.first()
        val title = animeData.title
        val posterUrl = animeData.images.jpg.imageUrl
        val rating = animeData.score
        val year = animeData.year
        val description = animeData.synopsis
        val url = animeData.url

        val data = LoadLinksData(
            title,
            malId,
            year,
        ).toJson()
        return newMovieLoadResponse(title, url, TvType.Movie, data) {
            this.posterUrl = posterUrl
            this.plot = description
            this.rating = rating
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        
        return true
    }

    data class PassData(
        val malId: Int
    )

    data class LoadLinksData(
        val title: String,
        val malId: Int,
        val year: Int
    )

    data class AnimeResponse(
        val data: List<AnimeData>,
        val pagination: Pagination?
    )

    data class Pagination(
        val lastVisiblePage: Int,
        val hasNextPage: Boolean,
        val currentPage: Int,
        val items: Items
    )

    data class Items(
        val count: Int,
        val total: Int,
        val perPage: Int
    )

    data class AnimeData(
        val malId: Int,
        val url: String,
        val images: Images,
        val trailer: Trailer,
        val approved: Boolean,
        val titles: List<Title>,
        val title: String,
        val titleEnglish: String,
        val titleJapanese: String,
        val titleSynonyms: List<String>,
        val type: String,
        val source: String,
        val episodes: Int,
        val status: String,
        val airing: Boolean,
        val aired: Aired,
        val duration: String,
        val rating: String,
        val score: Double,
        val scoredBy: Int,
        val rank: Int,
        val popularity: Int,
        val members: Int,
        val favorites: Int,
        val synopsis: String,
        val background: String,
        val season: String,
        val year: Int,
        val broadcast: Broadcast,
        val producers: List<Producer>,
        val licensors: List<Licensor>,
        val studios: List<Studio>,
        val genres: List<Genre>,
        val explicitGenres: List<Genre>,
        val themes: List<Theme>,
        val demographics: List<Demographic>
    )

    data class Images(
        val jpg: Image,
        val webp: Image
    )

    data class Image(
        val imageUrl: String,
        val smallImageUrl: String,
        val largeImageUrl: String
    )

    data class Trailer(
        val youtubeId: String,
        val url: String,
        val embedUrl: String
    )

    data class Title(
        val type: String,
        val title: String
    )

    data class Aired(
        val from: String,
        val to: String,
        val prop: Prop
    )

    data class Prop(
        val from: Date,
        val to: Date,
        val string: String
    )

    data class Date(
        val day: Int,
        val month: Int,
        val year: Int
    )

    data class Broadcast(
        val day: String,
        val time: String,
        val timezone: String,
        val string: String
    )

    data class Producer(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )

    data class Licensor(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )

    data class Studio(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )

    data class Genre(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )

    data class Theme(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )

    data class Demographic(
        val malId: Int,
        val type: String,
        val name: String,
        val url: String
    )
}