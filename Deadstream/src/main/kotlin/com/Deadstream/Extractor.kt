package com.Deadstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.Voe
import com.lagradost.cloudstream3.extractors.StreamWishExtractor

class MyFileMoon : Filesim() {
    override var mainUrl = "https://filemoon.nl"
}

class Voesx : Voe() {
    override var mainUrl = "https://voe.sx"
}

class Strwish : StreamWishExtractor() {
    override var mainUrl = "https://strwish.com"
}
