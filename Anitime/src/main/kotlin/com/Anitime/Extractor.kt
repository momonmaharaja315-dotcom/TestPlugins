package com.Anitime
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import org.json.JSONObject
import java.util.regex.Pattern

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = true

    private fun toStringCases(txtResult: String): String {
        var sumBase = ""
        var m3 = false
        if (".toString(" in txtResult) {
            if ("+(" in txtResult) {
                m3 = true
                try {
                    sumBase = "+" + Regex(r".toString...(\d+).").find(txtResult)?.groupValues?.get(1)
                } catch (e: Exception) {
                    sumBase = ""
                }
                val txtPreTemp = Regex(r"..(\d),(\d+).").findAll(txtResult).map { it.destructured.to(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
                val txtTemp = txtPreTemp.map { (b, n) -> n to b }
            } else {
                val txtTemp = Regex(r'(\d+)\.0.\w+.([^\)]+).').findAll(txtResult).map { it.destructured.to(it.groupValues[1].toInt(), it.groupValues[2]) }
            }
            for ((numero, base) in txtTemp) {
                val code = toString(numero, eval("$base$sumBase"))
                txtResult = if (m3) {
                    Regex(r'"|\+').replace(txtResult, "").replace("($base,$numero)", code)
                } else {
                    Regex(r"'|\+").replace(txtResult, "").replace("$numero.0.toString($base)", code)
                }
            }
        }
        return txtResult
    }

    private fun toString(number: Int, base: Int): String {
        val string = "0123456789abcdefghijklmnopqrstuvwxyz"
        return if (number < base) {
            string[number]
        } else {
            toString(number / base, base) + string[number % base]
        }
    }

    private fun decode(text: String): String {
        val cleanedText = text.replace(Regex("\\s+|/\\*.*?\\*/"), "")
        val decodedText = if ("(ﾟɆﾟ)" in cleanedText) {
            val data = cleanedText.split("+(ﾟɆﾟ)[ﾟoﾟ]")[1]
            val chars = data.split("+(ﾟɆﾟ)[ﾟεﾟ]+").drop(1)
            val char1 = "ღ"
            val char2 = "(ﾟɆﾟ)[ﾟΘﾟ]"
        } else {
            val data = cleanedText.split("+(ﾟДﾟ)[ﾟoﾟ]")[1]
            val chars = data.split("+(ﾟДﾟ)[ﾟεﾟ]+").drop(1)
            val char1 = "c"
            val char2 = "(ﾟДﾟ)['0']"
        }

        val txt = chars.map { char ->
            var c = char
            c = c.replace("(oﾟｰﾟo)", "u")
                .replace(char1, "0")
                .replace(char2, "c")
                .replace("ﾟΘﾟ", "1")
                .replace("!+[]", "1")
                .replace("-~", "1+")
                .replace("o", "3")
                .replace("_", "3")
                .replace("ﾟｰﾟ", "4")
                .replace("(+", "(")
            c = Pattern.compile(r'\((\d)\)').matcher(c).replaceAll { it.group(1) }

            var subchar = ""
            for (v in c) {
                subchar += v
                try {
                    subchar += eval(subchar).toString()
                    subchar = ""
                } catch (e: Exception) {
                }
            }
            subchar.replace('+', '')
        }.joinToString("|")

        val txtResult = txt.split('|').map { n ->
            Integer.parseInt(n, 8).toChar()
        }.joinToString("")

        return toStringCases(txtResult)
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val scriptTags = document.select("script")
        val script = scriptTags.lastOrNull().toString()

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                script,
                referer = "",
                Qualities.Unknown.value
            )
        )
        // val responseText = document.toString()
        // val base64Pattern = Regex("PLAYER\\(atob\\(\"(.*?)\"\\)")
        // val base64Value = base64Pattern.find(responseText)?.groups?.get(1)?.value ?: ""
        // val decodedJson = base64Decode(base64Value)
        // val jsonObject = JSONObject(decodedJson)

        // val domain = jsonObject.getString("domain")
        // val vidId = jsonObject.getString("id")
        // val videoUrls = mapOf(
        //     "360p" to "https://$domain/$vidId",
        //     "720p" to "https://$domain/www$vidId",
        //     "1080p" to "https://$domain/whw$vidId"
        // )
        // val headers = mapOf(
        //     "Referer" to "$mainUrl",
        //     "Sec-Fetch-Mode" to "cors"
        // )

        // for ((quality, link) in videoUrls) {
        //     callback.invoke (
        //         ExtractorLink (
        //             this.name,
        //             this.name,
        //             link,
        //             referer = mainUrl,
        //             getQualityFromName(quality),
        //             headers = headers
        //         )
        //     )
        // }
    }
}
