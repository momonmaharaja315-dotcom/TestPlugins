package com.megix
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.FormBody
import org.json.JSONObject
import java.net.URI
import org.jsoup.nodes.Document

fun fixUrl(url: String, domain: String): String {
    if (url.startsWith("http")) {
        return url
    }
    if (url.isEmpty()) {
        return ""
    }

    val startsWithNoHttp = url.startsWith("//")
    if (startsWithNoHttp) {
        return "https:$url"
    } else {
        if (url.startsWith('/')) {
            return domain + url
        }
        return "$domain/$url"
    }
}

fun getBaseUrl(url: String): String {
    return URI(url).let {
        "${it.scheme}://${it.host}"
    }
}

suspend fun bypass(url: String): String? {
    fun Document.getFormUrl(): String {
        return this.select("form#landing").attr("action")
    }

    fun Document.getFormData(): Map<String, String> {
        return this.select("form#landing input").associate { it.attr("name") to it.attr("value") }
    }

    val host = getBaseUrl(url)
    var res = app.get(url).document
    var formUrl = res.getFormUrl()
    var formData = res.getFormData()

    res = app.post(formUrl, data = formData).document
    formUrl = res.getFormUrl()
    formData = res.getFormData()

    res = app.post(formUrl, data = formData).document
    val skToken = res.selectFirst("script:containsData(?go=)")?.data()?.substringAfter("?go=")
        ?.substringBefore("\"") ?: return null
    val driveUrl = app.get(
        "$host?go=$skToken", cookies = mapOf(
            skToken to "${formData["_wp_http2"]}"
        )
    ).document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
    val path = app.get(driveUrl ?: return null).text.substringAfter("replace(\"")
        .substringBefore("\")")
    if (path == "/404") return null
    return fixUrl(path, getBaseUrl(driveUrl))
}

class Driveseed : ExtractorApi() {
    override val name: String = "Driveseed"
    override val mainUrl: String = "https://driveseed.org"
    override val requiresReferer = false

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }

    private suspend fun CFType1(url: String): String? {
        val cfWorkersLink = url.replace("/file", "/wfile") + "?type=1"
        val document = app.get(cfWorkersLink).document
        val link = document.selectFirst("a.btn-success").attr("href")
        return link ?: null
    }

    private suspend fun CFType2(url: String): String? {
        val cfWorkersLink = url.replace("/file", "/wfile") + "?type=2"
        val document = app.get(cfWorkersLink).document
        val link = document.selectFirst("a.btn-success").attr("href")
        return link ?: null
    }

    private suspend fun resumeCloudLink(url: String): String? {
        val resumeCloudUrl = "https://driveseed.org" + url
        val document = app.get(resumeCloudUrl).document
        val link = document.selectFirst("a.btn-success").attr("href")
        return link ?: null
    }

    private suspend fun resumeBot(url : String): String? {
        val resumeBotResponse = app.get(url)
        val resumeBotDoc = resumeBotResponse.document.toString()
        val ssid = resumeBotResponse.cookies["PHPSESSID"]
        val resumeBotToken = Regex("formData\\.append\\('token', '([a-f0-9]+)'\\)").find(resumeBotDoc)?.groups?.get(1)?.value
        val resumeBotPath = Regex("fetch\\('/download\\?id=([a-zA-Z0-9/\\+]+)'").find(resumeBotDoc)?.groups?.get(1)?.value
        val resumeBotBaseUrl = url.split("/download")[0]
        val requestBody = FormBody.Builder()
            .addEncoded("token", "$resumeBotToken")
            .build()

        val jsonResponse = app.post(resumeBotBaseUrl + "/download?id=" + resumeBotPath,
            requestBody = requestBody,
            headers = mapOf(
                "Accept" to "*/*",
                "Origin" to "$resumeBotBaseUrl",
                "Sec-Fetch-Site" to "same-origin"
            ),
            cookies = mapOf("PHPSESSID" to "$ssid"),
            referer = url
        ).text
        val jsonObject = JSONObject(jsonResponse)
        val link = jsonObject.getString("url")
        return link ?: null
    }

    private suspend fun instantLink(url: String): String? {
        val token = url.split("=").getOrNull(1) ?: ""
        val videoSeedUrl = url.split("/").take(3).joinToString("/") + "/api"
        val requestBody = FormBody.Builder().add("keys", "$token").build()
        val downloadlink = app.post(
            url = videoSeedUrl,
            requestBody = requestBody,
            headers = mapOf(
                "x-token" to "$videoSeedUrl",
            )
        )

        val finaldownloadlink =
            downloadlink.toString().substringAfter("url\":\"")
                .substringBefore("\",\"name")
                .replace("\\/", "/")

        return finaldownloadlink ?: null
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val quality = document.selectFirst("li.list-group-item:contains(Name)").text()

        val resumeBotUrl = document.selectFirst("a.btn.btn-light").attr("href")
        val resumeLink = resumeBot(resumeBotUrl)
        if (resumeLink != null) {
            callback.invoke(
                ExtractorLink(
                    "ResumeBot",
                    "ResumeBot(VLC)",
                    resumeLink,
                    "",
                    getIndexQuality(quality)
                )
            )
        }

        val resumeCloudUrl = document.selectFirst("a.btn-warning").attr("href")
        val resumeCloud = resumeCloudLink(resumeCloudUrl)
        if (resumeCloud != null) {
            callback.invoke(
                ExtractorLink(
                    "ResumeCloud",
                    "ResumeCloud",
                    resumeCloud,
                    "",
                    getIndexQuality(quality)
                )
            )
        }

        val cfType1 = CFType1(url)
        if (cfType1 != null) {
            callback.invoke(
                ExtractorLink(
                    "CF Type1",
                    "CF Type1",
                    cfType1,
                    "",
                    getIndexQuality(quality)
                )
            )
        }

        val instantUrl = document.select("a.btn-danger").attr("href")
        val instant = instantLink(instantUrl)
        if (instant != null) {
            callback.invoke(
                ExtractorLink(
                    "Instant",
                    "Instant(Download)",
                    instant,
                    "",
                    getIndexQuality(quality)
                )
            )
        }

        // val cfType2 = CFType2(url)
        // if (cfType2 != null) {
        //     callback.invoke(
        //         ExtractorLink(
        //             "CF Type2",
        //             "CF Type2",
        //             cfType2,
        //             "",
        //             getIndexQuality(quality)
        //         )
        //     )
        // }
    }
}
