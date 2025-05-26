package com.megix

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class WIMP_Provider: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WIMP())
        registerExtractorAPI(Doodporn())
        registerExtractorAPI(Filemoonlink())
    }
}
