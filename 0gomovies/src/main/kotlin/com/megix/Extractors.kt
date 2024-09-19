package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.Jeniusplay
import com.lagradost.cloudstream3.extractors.Odnoklassniki

class Asumanaksoy: Jeniusplay() {
    override var mainUrl = "https://cdn.asumanaksoy.com"
}

class OkRu : Odnoklassniki() {
    override var name = "OkRu"
    override var mainUrl = "https://ok.ru"
}

