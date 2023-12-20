plugins {
    java
    application
}

group = "me.duncte123"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Include the lavalink client
    implementation(projects.lavalinkClient)

    // other libs such as a discord client and a logger
    implementation(libs.jda)
    implementation(libs.logger.impl)
}
