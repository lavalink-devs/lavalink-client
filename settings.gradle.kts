rootProject.name = "lavalink-client"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            versionRefs()
            common()
            discordLibs()
            testLibs()
        }
    }
}

fun VersionCatalogBuilder.versionRefs() {
    version("kotlin", "1.9.21")
    version("dokka", "1.9.10")
    version("grgit", "5.2.0")
    version("maven-publish", "0.25.3")

    version("logger", "2.0.7")
}

fun VersionCatalogBuilder.common() {
    library("kotlin", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
    library("lavalink-protocol", "dev.arbjerg.lavalink", "protocol").version("4.0.0")
    library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.10.0")
    library("reactor-core", "io.projectreactor", "reactor-core").version("3.5.6")
    library("reactor-kotlin", "io.projectreactor.kotlin", "reactor-kotlin-extensions").version("1.2.2")

    bundle("reactor", listOf("reactor-core", "reactor-kotlin"))

    library("logger-api", "org.slf4j", "slf4j-api").versionRef("logger")
}

fun VersionCatalogBuilder.discordLibs() {
    library("jda", "net.dv8tion", "JDA").version("5.0.0-beta.11")
    library("d4j", "com.discord4j", "discord4j-core").version("3.2.3")
}

fun VersionCatalogBuilder.testLibs() {
    library("logger-impl", "org.slf4j", "slf4j-simple").versionRef("logger")
    library("lavasearch", "com.github.topi314.lavasearch", "lavasearch-protocol").version("1.0.0-beta.2")
}
