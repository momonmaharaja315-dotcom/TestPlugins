package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.google.gson.annotations.SerializedName
import okhttp3.*

class CinemaluxeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://luxecinema.fans"
    override var name = "Cinemaluxe"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/genre/hollywood/page/" to "Hollywood Movies",
        "$mainUrl/genre/south-indian-movies/page/" to "South Indian Movies",
        "$mainUrl/genre/hollywood-tv-show/page/" to "Hollywood TV Shows",
        "$mainUrl/genre/bollywood-tv-show/page/" to "Bollywood TV Shows",
        "$mainUrl/genre/anime-tv-show/page/" to "Anime TV Shows",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    data class Options (
        @SerializedName("soralink_pleasewait_countdown_text" ) var soralinkPleasewaitCountdownText : Boolean? = null,
        @SerializedName("soralink_open_in_new_tab"           ) var soralinkOpenInNewTab            : Boolean? = null,
        @SerializedName("soralink_button_appearance_mode"    ) var soralinkButtonAppearanceMode    : String?  = null,
        @SerializedName("soralink_z"                         ) var soralinkZ                       : String?  = null,
        @SerializedName("soralink_mode"                      ) var soralinkMode                    : String?  = null,
        @SerializedName("soralink_auto_guessing_mode"        ) var soralinkAutoGuessingMode        : Boolean? = null,
        @SerializedName("rechaptcha_enabled"                 ) var rechaptchaEnabled               : Boolean? = null,
        @SerializedName("googlekey"                          ) var googlekey                       : String?  = null,
        @SerializedName("grechaptcha_element_id"             ) var grechaptchaElementId            : String?  = null,
        @SerializedName("grechaptcha_skin"                   ) var grechaptchaSkin                 : String?  = null,
        @SerializedName("soralink_ajaxurl"                   ) var soralinkAjaxurl                 : String?  = null
    )

    data class Item (
        @SerializedName("token"    ) var token    : String?  = null,
        @SerializedName("id"       ) var id       : Int?     = null,
        @SerializedName("time"     ) var time     : Int?     = null,
        @SerializedName("post"     ) var post     : String?  = null,
        @SerializedName("redirect" ) var redirect : String?  = null,
        @SerializedName("cacha"    ) var cacha    : String?  = null,
        @SerializedName("new"      ) var new      : Boolean? = null,
        @SerializedName("link"     ) var link     : String?  = null
    )

    private suspend fun bypass(url: String): String {
        val document = app.get(url, allowRedirects = true).document.toString()
        //val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
        val itemRegex = """var item = (\{.*?\})"""
        val optionsRegex = """var options = (\{.*?\});"""
        val optionsMatch = optionsRegex.find(document)
        val itemMatch = itemRegex.find(document)

        if (itemMatch != null && optionsMatch != null) {
            val itemObject = itemMatch.groupValues[1]
            val optionsObject = optionsMatch.groupValues[1]
            val itemData = parseJson<Item>(itemObject)
            val optionsData = parseJson<Options>(optionsObject)
            val url = makeRequest(itemData, optionsData, 0)
            return url
        }
        return "empty"
    }


    suspend fun makeRequest(itemData : Item, optionsData: Options, try: Integer) : String {
        val formBody = FormBody.Builder().apply {
            itemData::class.members.forEach { member ->
                if (member is kotlin.reflect.KProperty<*>) {
                    val value = member.getter.call(itemData)?.toString()
                    if (value != null) {
                        add(member.name, value)
                    }
                }
            }
            add("action", optionsData.soralinkZ ?: "")
        }.build()

        val headers = mapOf(
            "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "accept-language" to "en-US,en;q=0.9,en-IN;q=0.8",
            "cache-control" to "no-cache",
            "content-type" to "application/x-www-form-urlencoded",
            "pragma" to "no-cache",
            "priority" to "u=0, i",
            "sec-ch-ua" to "\"Not(A:Brand\";v=\"99\", \"Microsoft Edge\";v=\"133\", \"Chromium\";v=\"133\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "sec-fetch-dest" to "document",
            "sec-fetch-mode" to "navigate",
            "sec-fetch-site" to "same-origin",
            "sec-fetch-user" to "?1",
            "upgrade-insecure-requests" to "1",
            "cookie" to "ext_name=ojplmecpdpgccookcobabopnaifgidhf",
            "Referer" to "https://hdmovie.website/future-of-blockchain-2025/",
            "Referrer-Policy" to "strict-origin-when-cross-origin"
        )

        val response = app.post(
            itemData.redirect ?: "",
            body = formBody,
            headers = headers,
            allowRedirects = false
        )
        if(response?.status >= 300 && response?.status < 400) {
            return response.headers["location"] ?: "empty"
        }
        else if(response?.status == 200) {
            if(try < 6) {
                makeRequest(itemData, optionsData, try + 1)
            }
            else {
                return "empty"
            }
        }
        else {
            return "empty"
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img") ?. attr("alt") ?: ""
        val href = this.selectFirst("a") ?. attr("href") ?: ""
        val posterUrl = this.selectFirst("img") ?. attr("data-src") ?: ""
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..6) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.result-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.sheader > div.data > h1")?.text().toString()
        var posterUrl = document.selectFirst("div.sheader noscript img")?.attr("src")
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        }
        var description = document.selectFirst("div[itemprop=description]")?.ownText()
        if(description.isNullOrEmpty()) {
            description = document.selectFirst("div.wp-content")?.text()
        }

        val tvType = if (url.contains("tvshow")) {
            "series"
        } else {
            "movie"
        }

        if(tvType == "series") {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var hTags = document.select("h3:matches((?i)(4K|[0-9]*0p))")
            if(hTags.isEmpty()) {
                hTags = document.select("a.maxbutton-5")
            }
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()

            hTags.mapNotNull{ hTag ->
                val seasonText = hTag.text()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val matchResult = realSeasonRegex.find(seasonText.toString())
                val realSeason = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                var seasonLink =  if(hTag.tagName() == "a") {
                    hTag.attr("href")
                }
                else {
                    val spanTag = hTag.nextElementSibling()
                    spanTag ?.selectFirst("a")?.attr("href").toString()
                }
                seasonLink = bypass(seasonLink)
                val doc = app.get(seasonLink).document
                var aTags = doc.select("a.maxbutton:matches((?i)(Episode))")
                
                aTags.mapNotNull { aTag ->
                    val epText = aTag.text()
                    val e = Regex("""(?i)(?:episode\s*[-]?\s*)(\d{1,2})""").find(epText)?.groups?.get(1)?.value ?.toIntOrNull() ?: 0
                    val epUrl = aTag.attr("href")
                    val key = Pair(realSeason, e)
                    if (episodesMap.containsKey(key)) {
                        val currentList = episodesMap[key] ?: emptyList()
                        val newList = currentList.toMutableList()
                        newList.add(epUrl)
                        episodesMap[key] = newList
                    } else {
                        episodesMap[key] = mutableListOf(epUrl)
                    }
                }
            }

            for ((key, value) in episodesMap) {
                val data = value.map { source->
                    EpisodeLink(
                        source
                    )
                }
                tvSeriesEpisodes.add(
                    newEpisode(data) {
                        this.season = key.first
                        this.episode = key.second
                    }
                )
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
        else {
            val buttons = document.select("a.maxbutton")
            val data = buttons.flatMap { button ->
                var link = button.attr("href")
                link = bypass(link)
                val doc = app.get(link).document
                val selector = if(link.contains("luxedrive")) "div > a" else "a.maxbutton"
                doc.select(selector).mapNotNull {
                    val source = it.attr("href")
                    EpisodeLink(
                        source
                    )
                }
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources = parseJson<ArrayList<EpisodeLink>>(data)
        sources.amap {
            val source = it.source
            loadExtractor(source, subtitleCallback, callback)
        }
        return true   
    }

    data class EpisodeLink(
        val source: String
    )
}
