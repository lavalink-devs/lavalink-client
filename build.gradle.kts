import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "dev.arbjerg"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.arbjerg.dev/releases")
    maven("https://maven.arbjerg.dev/snapshots")
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    // package libraries
    api("dev.arbjerg.lavalink:protocol:004a8f873517e1bf7771585ec915b4360257acd2-SNAPSHOT")
    api("com.neovisionaries:nv-websocket-client:2.14")
    api("com.squareup.okhttp3:okhttp:4.10.0")
    api("io.projectreactor:reactor-core:3.5.6")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")
    api("org.slf4j:slf4j-api:2.0.7")

    // Discord library support
    compileOnly("net.dv8tion:JDA:5.0.0-beta.11")
    compileOnly("com.discord4j:discord4j-core:3.2.3")

    testImplementation(kotlin("test"))
    testImplementation("net.dv8tion:JDA:5.0.0-beta.11")
    testImplementation("com.discord4j:discord4j-core:3.2.3")
    testImplementation("org.slf4j:slf4j-simple:2.0.7")
}

val sourcesForRelease = task<Copy>("sourcesForRelease") {
    from("src/main/kotlin") {
        include("**/Version.java")

        filter<ReplaceTokens>(mapOf("tokens" to mapOf(
            "gradle_plugin_ver_to_annoy_schlaudbibus" to project.version
        )))
    }
    into("build/filteredSrc")

    includeEmptyDirs = false
}

val generateKotlinSources = task<SourceTask>("generateKotlinSources") {
    val javaSources = sourceSets["main"].allSource.filter {
        it.name != "Version.java"
    }.asFileTree

    source = javaSources + fileTree(sourcesForRelease.destinationDir)
    dependsOn(sourcesForRelease)
}

tasks.compileJava {
    source = generateKotlinSources.source
    dependsOn(generateKotlinSources)
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
