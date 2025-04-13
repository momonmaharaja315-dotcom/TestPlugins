package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.metaproviders.TraktProvider


class CineTraktProvider : TraktProvider() {
    override var name = "Trakt(CineStream)"
}
