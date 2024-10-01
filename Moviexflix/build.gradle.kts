version = 1

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Includes HDMoviesflix"
     authors = listOf("megix")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime"
    )

    iconUrl = "https://themoviezflix.cn.com/wp-content/uploads/2024/04/cropped-favicon-32x32-1.png"
}
