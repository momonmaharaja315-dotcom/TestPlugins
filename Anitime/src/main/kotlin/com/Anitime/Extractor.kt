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

    private fun fetchTodecode(url: String): String? {
        val response = app.get(url).document
        val scripts = response.select("script:contains(━┻)")
        for (script in scripts) {
            if (script.html().contains("━┻")) {
                return script.html()
            }
        }
        return null
    }

    private fun decode(text: String): String {
        var cleanedText = text.replace(Regex("\\s+|/\\*.*?\\*/"), "").toString()
        val (data, chars, char1, char2) = if (cleanedText.contains("(ﾟɆﾟ)")) {
            val parts = cleanedText.split("+(ﾟɆﾟ)[ﾟoﾟ]")
            val chars = parts[1].split("+(ﾟɆﾟ)[ﾟεﾟ]+").drop(1)
            Pair(parts[1], chars, "ღ", "(ﾟɆﾟ)[ﾟΘﾟ]")
        } else {
            val parts = cleanedText.split("+(ﾟДﾟ)[ﾟoﾟ]")
            val chars = parts[1].split("+(ﾟДﾟ)[ﾟεﾟ]+").drop(1)
            Pair(parts[1], chars, "c", "(ﾟДﾟ)['0']")
        }

        var txt = ""
        for (char in chars) {
            var modifiedChar = char
                .replace("(oﾟｰﾟo)", "u")
                .replace(char1, "0")
                .replace(char2, "c")
                .replace("ﾟΘﾟ", "1")
                .replace("!+[]", "1")
                .replace("-~", "1+")
                .replace("o", "3")
                .replace("_", "3")
                .replace("ﾟｰﾟ", "4")
                .replace("(+", "(")

            modifiedChar = Regex("\\((\\d)\\)").replace(modifiedChar) { it.groupValues[1] }

            var c = ""
            var subchar = ""
            for (v in modifiedChar) {
                c += v
                try {
                    val x = c
                    subchar += eval(x).toString()
                    c = ""
                } catch (e: Exception) {
                    // Ignore exceptions
                }
            }
            if (subchar.isNotEmpty()) {
                txt += "$subchar|"
            }
        }
        txt = txt.dropLast(1).replace('+', ' ')

        val txtResult = txt.split('|').joinToString("") { it.toInt(8).toChar().toString() }

        return toStringCases(txtResult)
    }

    private fun toStringCases(txtResult: String): String {
        var sumBase = ""
        var m3 = false
        var result = txtResult

        if (result.contains(".toString(")) {
            if (result.contains("+(")) {
                m3 = true
                sumBase = Regex(".toString...\\d+").find(result)?.groups?.get(1)?.value ?: ""
                val txtPreTemp = Regex("..(\\d),(\\d+)").findAll(result).map { it.destructured }.toList()
                val txtTemp = txtPreTemp.map { (n, b) -> b to n }
            } else {
                val txtTemp = Regex("(\\d+)\\.0\\.\\w+\\.([^\\)]+)").findAll(result)
            }
            for ((numero, base) in txtTemp) {
                val code = toString(numero.toInt(), eval(base + sumBase))
                result = if (m3) {
                    result.replace("\"|\\+", "").replace("($base,$numero)", code)
                } else {
                    result.replace("'|\\+", "").replace("$numero.0.toString($base)", code)
                }
            }
        }
        return result
    }

    private fun toString(number: Int, base: Int): String {
        val string = "0123456789abcdefghijklmnopqrstuvwxyz"
        return if (number < base) {
            string[number].toString()
        } else {
            toString(number / base, base) + string[number % base]
        }
    }

    private fun eval(code: String): Int {
        return try {
            code.toInt()
        } catch (e: Exception) {
            eval(code)
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        //val document = app.get(url).document
        //val responseText = document.toString()
        val decodeString = fetchTodecode(url)
        val base64Pattern = Regex("PLAYER\\(atob\\(\"(.*?)\"\\)")
        val base64Value = base64Pattern.find(decodeString)?.groups?.get(1)?.value ?: ""
        val decodedJson = base64Decode(base64Value)
        val jsonObject = JSONObject(decodedJson)

        val domain = jsonObject.getString("domain")
        val vidId = jsonObject.getString("id")
        val videoUrls = mapOf(
            "360p" to "https://$domain/$vidId",
            "720p" to "https://$domain/www$vidId",
            "1080p" to "https://$domain/whw$vidId"
        )
        val headers = mapOf(
            "Referer" to "$mainUrl",
            "Sec-Fetch-Mode" to "cors"
        )

        for ((quality, link) in videoUrls) {
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    link,
                    referer = mainUrl,
                    getQualityFromName(quality),
                    headers = headers
                )
            )
        }
    }
}
