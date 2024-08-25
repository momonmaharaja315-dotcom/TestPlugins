package com.Deadstream

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractor.VidHidePro
import com.lagradost.cloudstream3.extractor.Voe
import com.lagradost.cloudstream3.extractor.StreamWishExtractor
import com.lagradost.cloudstream3.extractor.Chillx

@CloudstreamPlugin
class DeadstreamProvider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Deadstream())
        registerExtractorAPI(Chillx())
        registerExtractorAPI(Voe())
        registerExtractorAPI(StreamWishExtractor())
        registerExtractorAPI(MyFileMoon())
        registerExtractorAPI(VidHidePro())
        //registerExtractorAPI(AbyssCdn())
    }
}
