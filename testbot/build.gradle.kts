plugins {
    java
    application
    kotlin("jvm")
}

group = "me.duncte123"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Include the lavalink client
    implementation(projects.lavalinkClient)

    // implementation(kotlin("stdlib")) // Do we need this?
    implementation(kotlin("scripting-jsr223"))

    // other libs such as a discord client and a logger
    implementation(libs.jda)
    implementation(libs.logger.impl)
    implementation(libs.lyrics)
}
