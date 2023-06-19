plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
}

group = "dev.arbjerg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    // package libraries
    api("com.github.lavalink-devs.Lavalink:protocol:v4-SNAPSHOT")
    api("com.neovisionaries:nv-websocket-client:2.14")
    api("com.squareup.okhttp3:okhttp:4.10.0")
    api("io.projectreactor:reactor-core:3.5.6")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")

    // Discord library support
    compileOnly("net.dv8tion:JDA:5.0.0-beta.11")

    testImplementation(kotlin("test"))
    testImplementation("net.dv8tion:JDA:5.0.0-beta.11")
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
