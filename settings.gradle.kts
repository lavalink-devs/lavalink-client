rootProject.name = "lavalink-client"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":testbot")

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
    version("maven-publish", "0.32.0")
    version("lavalink", "4.0.3")

    version("logger", "2.0.7")
}

fun VersionCatalogBuilder.common() {
    library("kotlin", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
    library("jackson", "com.fasterxml.jackson.core", "jackson-core").version("2.16.0")
    library("lavalink-protocol", "dev.arbjerg.lavalink", "protocol").versionRef("lavalink")
    library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.10.0")
    library("reactor-core", "io.projectreactor", "reactor-core").version("3.5.6")
    library("reactor-kotlin", "io.projectreactor.kotlin", "reactor-kotlin-extensions").version("1.2.2")

    bundle("reactor", listOf("reactor-core", "reactor-kotlin"))

    library("logger-api", "org.slf4j", "slf4j-api").versionRef("logger")
}

fun VersionCatalogBuilder.discordLibs() {
    library("jda", "net.dv8tion", "JDA").version("5.1.0")
    library("d4j", "com.discord4j", "discord4j-core").version("3.2.3")
}

fun VersionCatalogBuilder.testLibs() {
    library("logger-impl", "ch.qos.logback", "logback-classic").version("1.4.14")
    library("lyrics", "com.github.DuncteBot.java-timed-lyrics", "protocol").version("1.2.0")
    library("lavasearch", "com.github.topi314.lavasearch", "lavasearch-protocol").version("1.0.0-beta.2")
}
