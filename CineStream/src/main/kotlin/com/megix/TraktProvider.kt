package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.metaproviders.TraktProvider

class CineTraktProvider : TraktProvider() {
    override var name = "Trakt(CineStream)"

    override val mainPage =
            mainPageOf(
                    "$traktApiUrl/movies/trending?extended=cloud9,full&limit=25" to
                            "Trending Movies",
                    "$traktApiUrl/movies/popular?extended=cloud9,full&limit=25" to "Popular Movies",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25" to "Trending Shows",
                    "$traktApiUrl/shows/popular?extended=cloud9,full&limit=25" to "Popular Shows",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=53,1465" to
                            "Netflix",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=47,2385" to
                            "Amazon Prime Video",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=256" to
                            "Apple TV+",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=41,2018,2566,2567,2597" to
                            "Disney+",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=87" to
                            "Hulu",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=1623" to
                            "Paramount+",
                    "$traktApiUrl/shows/trending?extended=cloud9,full&limit=25&network_ids=550,3027" to
                            "Peacock",
            )

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = AppUtils.parseJson<UltimaLinkData>(data)
        callback.invoke(
            newExtractorLink(
                "Trakt",
                "Trakt",
                mediaData.toString(),
            )
        )
        return true
    }
}
