import org.jetbrains.kotlin.konan.properties.Properties

version = 1

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "CONSUMET_API", "\"${properties.getProperty("CONSUMET_API")}\"")
    }
}

cloudstream {
    //language = "en"
    description = "Multi API Extension"
    authors = listOf("megix")
    status = 1
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime"
    )

    iconUrl = "https://cdn.vectorstock.com/i/1000x1000/45/88/at-letter-logo-design-with-simple-style-vector-32194588.jpg"
}
