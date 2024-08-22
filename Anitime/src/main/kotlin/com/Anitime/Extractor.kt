package com.Anitime

import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import org.json.JSONObject

class Boosterx : Chillx() {
    override val name = "Boosterx"
    override val mainUrl = "https://boosterx.stream"
}

class AbyssCdn : ExtractorApi() {
    override val name: String = "AbyssCdn"
    override val mainUrl: String = "https://abysscdn.com"
    override val requiresReferer = true

    private fun customDecode(input: String): String {
        val charset = "RB0fpH8ZEyVLkv7c2i6MAJ5u3IKFDxlS1NTsnGaqmXYdUrtzjwObCgQP94hoeW+/="
        var output = StringBuilder()
        var i = 0
        var enc1: Int
        var enc2: Int
        var enc3: Int
        var enc4: Int

        var cleanedInput = input.replace("_", "")
        cleanedInput = cleanedInput.replace(Regex("[^A-Za-z0-9+/=]"), "")

        while (i < cleanedInput.length) {
            enc1 = charset.indexOf(cleanedInput[i++])
            enc2 = charset.indexOf(cleanedInput[i++])
            enc3 = charset.indexOf(cleanedInput[i++])
            enc4 = charset.indexOf(cleanedInput[i++])

            enc1 = (enc1 shl 2) or (enc2 shr 4)
            enc2 = ((enc2 and 15) shl 4) or (enc3 shr 2)
            enc3 = ((enc3 and 3) shl 6) or enc4

            output.append(enc1.toChar())
            if (enc3 != 64) {
                output.append(enc2.toChar())
            }
            if (enc4 != 64) {
                output.append(enc3.toChar())
            }
        }

        return decodeUtf8(output.toString())
    }

    private fun decodeUtf8(utf8String: String): String {
        val decodedString = StringBuilder()
        var i = 0

        while (i < utf8String.length) {
            val charCode = utf8String[i].toInt()

            when {
                charCode < 128 -> {
                    decodedString.append(utf8String[i])
                    i++
                }
                charCode > 191 && charCode < 224 -> {
                    val charCode2 = utf8String[i + 1].toInt()
                    decodedString.append(
                        ((charCode and 31) shl 6 or (charCode2 and 63)).toChar()
                    )
                    i += 2
                }
                else -> {
                    val charCode2 = utf8String[i + 1].toInt()
                    val charCode3 = utf8String[i + 2].toInt()
                    decodedString.append(
                        ((charCode and 15) shl 12 or ((charCode2 and 63) shl 6) or (charCode3 and 63)).toChar()
                    )
                    i += 3
                }
            }
        }

        return decodedString.toString()
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val responseText = document.toString()
        val base64Pattern = Regex("parse\\(atob\\(\"(.*?)\"\\)")
        val base64Value = base64Pattern.find(responseText)?.groups?.get(1)?.value ?: ""
        val decodedJson = customDecode(base64Value)
        val jsonObject = JSONObject(decodedJson)
        val domain = jsonObject.getString("domain")
        val vidId = jsonObject.getString("id")
        val videoUrls = mapOf(
            "360p" to "https://$domain/$vidId",
            "720p" to "https://$domain/www$vidId",
            "1080p" to "https://$domain/whw$vidId"
        )
        
        val headers = mapOf(
            "accept" to "*/*",
            "accept-encoding" to "gzip, deflate, br",
            "origin" to "https://abysscdn.com",
            "connection" to "keep-alive",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "cross-site"
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
