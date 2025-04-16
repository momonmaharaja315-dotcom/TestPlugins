version = 19

import org.jetbrains.kotlin.konan.properties.Properties

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        android.buildFeatures.buildConfig=true
        buildConfigField("String", "BYPASS_API", "\"${properties.getProperty("BYPASS_API")}\"")
    }
}

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Movies and Series upto 4K"
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

    iconUrl = "https://bollyflix-cdn.store/wp-content/uploads/2023/05/Bollyflix-movies.png"
}
