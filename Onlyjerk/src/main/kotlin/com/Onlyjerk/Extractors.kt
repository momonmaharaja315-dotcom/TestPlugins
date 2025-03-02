import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.DoodLaExtractor

class Dooodster : DoodLaExtractor() {
    override var mainUrl = "https://dooodster.com"
}

class Bigwrap : ExtractorApi() {
    override val name = "Bigwrap"
    override val mainUrl = "https://bigwrap.com"
    override val requiresReferer = true

    override fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = app.get(url, allowRedirects = false).headers["location"] ?: url
        val source = app.get(link).document().toString().substringAfter("""file:\"""").substringBefore("?t=")
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
