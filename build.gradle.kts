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
    // TODO: replace with official package when protocol gets released
    implementation("com.github.DRSchlaubi.Lavalink:protocol:7c03b48eec")
//    implementation("dev.arbjerg.lavalink")

    // TODO https://github.com/TakahikoKawasaki/nv-websocket-client

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
