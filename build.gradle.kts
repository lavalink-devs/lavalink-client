plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
}

group = "dev.arbjerg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.arbjerg.dev/releases")
    maven("https://maven.arbjerg.dev/snapshots")
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    // package libraries
    api("dev.arbjerg.lavalink:protocol:e3e3ef171359423eebcf37b50752d04c42a667d1-SNAPSHOT")
    api("com.neovisionaries:nv-websocket-client:2.14")
    api("com.squareup.okhttp3:okhttp:4.10.0")
    api("io.projectreactor:reactor-core:3.5.6")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")
    api("org.slf4j:slf4j-api:2.0.7")


    // Discord library support
    compileOnly("net.dv8tion:JDA:5.0.0-beta.11")
    // I have no clue how this lib works and the docs are confusing
    // compileOnly("com.discord4j:discord4j-core:3.2.5")

    testImplementation(kotlin("test"))
    testImplementation("net.dv8tion:JDA:5.0.0-beta.11")
    testImplementation("org.slf4j:slf4j-simple:2.0.7")
}

tasks.test {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "8.1.1"
    distributionType = Wrapper.DistributionType.BIN
}

kotlin {
    jvmToolchain(11)
}
