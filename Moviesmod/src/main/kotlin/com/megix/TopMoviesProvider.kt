package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlinx.coroutines.runBlocking

class TopmoviesProvider : MoviesmodProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://modflix.xyz/?type=bollywood"
    override var name = "TopMovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    companion object {
        val basemainUrl: String? by lazy {
            runBlocking {
                try {
                    val mainUrl = "https://modflix.xyz/?type=bollywood"
                    app.get(mainUrl).document
                        .selectFirst("meta[http-equiv=refresh]")?.attr("content")
                        ?.substringAfter("url=")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "$basemainUrl/page/" to "Home",
        "$basemainUrl/web-series/page/" to "Latest Web Series",
        "$basemainUrl/movies/hindi-movies/page/" to "Latest Hindi Movies",
    )
}
