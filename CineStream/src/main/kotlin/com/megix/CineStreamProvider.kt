package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.argamap
import com.lagradost.cloudstream3.network.CloudflareKiller

class CineStreamProvider : MainAPI() {
    override var mainUrl = "https://cinemeta-catalogs.strem.io"
    override var name = "CineStream"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io"
    val vegaMoviesAPI = "https://vegamovies.fans"
    val wpRedisInterceptor by lazy { CloudflareKiller() }
    val cfInterceptor = CloudflareKiller()
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
        val movies = parseJson<ArrayList<Home>>(json)
        val home = movies.mapNotNull { movie ->
            newMovieSearchResponse(movie.name, PassData(movie.id, movie.type).toJson(), TvType.Movie) {
                this.posterUrl = movie.poster.toString()
            }
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val movieJson = app.get("$cinemeta_url/catalog/movie/top/search=$query.json").text
        val movies = parseJson<SearchResult>(movieJson)
        movies.metas?.forEach {
            searchResponse.add(newMovieSearchResponse(it.name, PassData(it.id, it.type).toJson(), TvType.Movie) {
                this.posterUrl = it.poster.toString()
            })
        }

        val seriesJson = app.get("$cinemeta_url/catalog/series/top/search=$query.json").text
        val series = parseJson<SearchResult>(seriesJson)
        series.metas?.forEach {
            searchResponse.add(newMovieSearchResponse(it.name, PassData(it.id, it.type).toJson(), TvType.Movie) {
                this.posterUrl = it.poster.toString()
            })
        }

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

        if(tvtype == "movie") {
            val data = LoadLinksData(
                title,
                id,
                tvtype,
                year
            ).toJson()
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
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
        else {
            val episodes = movieData.meta?.videos?.map { ep ->
                newEpisode(
                    LoadLinksData(
                        title,
                        id,
                        tvtype,
                        year,
                        ep.season,
                        ep.episode,
                    ).toJson()
                ) {
                    this.name = ep.name ?: ep.title
                    this.season = ep.season
                    this.episode = ep.episode
                    this.posterUrl = ep.thumbnail
                    this.description = ep.overview
                }
            } ?: emptyList()

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
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
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = parseJson<LoadLinksData>(data)
        argamap(
            {
                invokeVegamovies(
                    res.title,
                    res.year,
                    res.season,
                    res.episode,
                    subtitleCallback,
                    callback
                )
            },
        )
        // callback.invoke(
        //     ExtractorLink(
        //         this.name,
        //         this.name,
        //         res.toString(),
        //         "",
        //         Qualities.Unknown.value,
        //     )
        // )
        return true
    }

    data class LoadLinksData(
        val title: String,
        val id: String,
        val tvtype: String,
        val year: String,
        val season: Int? = null,
        val episode: Int? = null,
    )

    data class PassData(
        val id: String,
        val type: String
    )

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

    data class SearchResult(
        val query: String,
        val rank: Double,
        val cacheMaxAge: Long,
        val metas: List<Media>?
    )

    data class Media(
        val id: String,
        val imdb_id: String,
        val type: String,
        val name: String,
        val releaseInfo: String?,
        val poster: String?,
        val slug: String?,
    )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int,
        val episode: Int,
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


    fun getEpisodeSlug(
        season: Int? = null,
        episode: Int? = null,
    ): Pair<String, String> {
        return if (season == null && episode == null) {
            "" to ""
        } else {
            (if (season!! < 10) "0$season" else "$season") to (if (episode!! < 10) "0$episode" else "$episode")
        }
    }

    fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }

    suspend fun loadCustomTagExtractor(
        tag: String? = null,
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        quality: Int? = null,
    ) {
        loadExtractor(url, referer, subtitleCallback) { link ->
            callback.invoke(
                ExtractorLink(
                    link.source,
                    "${link.name} $tag",
                    link.url,
                    link.referer,
                    when (link.type) {
                        ExtractorLinkType.M3U8 -> link.quality
                        else -> quality ?: link.quality
                    },
                    link.type,
                    link.headers,
                    link.extractorData
                )
            )
        }
    }


    suspend fun invokeVegamovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year.toIntOrNull,
            season,
            episode,
            subtitleCallback,
            callback,
            vegaMoviesAPI
        )
    }

    private suspend fun invokeWpredis(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val cfInterceptor = CloudflareKiller()
        val fixtitle = title?.substringBefore("-")?.substringBefore(":")?.replace("&", " ")
        val url = if (season == null) {
            "$api/search/$fixtitle $year"
        } else {
            "$api/search/$fixtitle season $season $year"
        }
        val domain= api.substringAfter("//").substringBefore(".")
        app.get(url, interceptor = cfInterceptor).document.select("#main-content article")
            .filter { element ->
                element.text().contains(
                    fixtitle.toString(), true
                )
            }
            .amap {
                val hrefpattern =
                    Regex("""(?i)<a\s+href="([^"]+)"[^>]*?>[^<]*?\b($fixtitle)\b[^<]*?""").find(
                        it.toString()
                    )?.groupValues?.get(1)
                if (hrefpattern!=null) {
                    val res = hrefpattern.let { app.get(it).document }
                    val hTag = if (season == null) "h5" else "h3,h5"
                    val aTag =
                        if (season == null) "Download Now" else "V-Cloud,Download Now,G-Direct"
                    val sTag = if (season == null) "" else "(Season $season|S$seasonSlug)"
                    val entries =
                        res.select("div.entry-content > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                            .filter { element ->
                                !element.text().contains("Series", true) &&
                                        !element.text().contains("Zip", true) &&
                                        !element.text().contains("[Complete]", true) &&
                                        !element.text().contains("480p, 720p, 1080p", true) &&
                                        !element.text().contains(domain, true) &&
                                        element.text().matches("(?i).*($sTag).*".toRegex())
                            }
                    entries.amap { it ->
                        val tags =
                            """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                                ?.trim()
                        val tagList = aTag.split(",")
                        val href = it.nextElementSibling()?.select("a")?.find { anchor ->
                            tagList.any { tag ->
                                anchor.text().contains(tag.trim(), true)
                            }
                        }?.attr("href") ?: ""
                        val selector =
                            if (season == null) "p a:matches(V-Cloud|G-Direct)" else "h4:matches(0?$episode) ~ p a:matches(V-Cloud|G-Direct)"
                        if (href.isNotEmpty()) {
                            app.get(
                                href, interceptor = wpRedisInterceptor
                            ).document.select("div.entry-content > $selector").first()?.let { sources ->
                                val server = sources.attr("href")
                                loadCustomTagExtractor(
                                    tags,
                                    server,
                                    "$api/",
                                    subtitleCallback,
                                    callback,
                                    getIndexQuality(it.text())
                                )
                            }
                        }
                    }
                }
            }
    }
}

