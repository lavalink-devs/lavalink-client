plugins {
    kotlin("jvm") version "1.8.21"
}

group = "dev.arbjerg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.lavalink-devs.Lavalink:protocol:v4-SNAPSHOT")
//    implementation("dev.arbjerg.lavalink")

    implementation("com.neovisionaries:nv-websocket-client:2.14")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    implementation("io.projectreactor:reactor-core:3.5.6")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}
