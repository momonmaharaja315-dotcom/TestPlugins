import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.DoodLaExtractor

open class Dooodster : DoodLaExtractor() {
    override var mainUrl = "https://dooodster.com"
}

open class Bigwarp : ExtractorApi() {
    override var name = "Bigwarp"
    override var mainUrl = "https://bigwarp.io"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = app.get(url, allowRedirects = false).headers["location"] ?: url
        val source = app.get(link).document.toString().substringAfter("""file:\"""").substringBefore("?t=")
        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                source,
                "",
                Qualities.Unknown.value
            )
        )
    }
}
