import org.apache.tools.ant.filters.ReplaceTokens
import org.ajoberstar.grgit.Grgit

plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("org.ajoberstar.grgit") version "5.2.0"
}

val (gitVersion, release) = versionFromGit()
logger.lifecycle("Version: $gitVersion (release: $release)")

group = "dev.arbjerg"
version = gitVersion
val archivesBaseName = "lavalink-client"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://maven.arbjerg.dev/releases")
    maven("https://maven.arbjerg.dev/snapshots")
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    // package libraries
    api("dev.arbjerg.lavalink:protocol:8a6c376407205e8208aa452c8b59443774c2e754-SNAPSHOT")
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

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
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
    jvmToolchain(17)
}

val isSnapshot = System.getenv("SNAPSHOT") == "true"

val mavenUrl: String
    get() {
        if (release) {
            return "https://maven.arbjerg.dev/releases"
        }

        return "https://maven.arbjerg.dev/snapshots"
    }

publishing {
    repositories {
        maven {
            name = "arbjerg"
            url = uri(mavenUrl)
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        register<MavenPublication>("arbjerg") {
            pom {
                name.set(archivesBaseName)
                description.set("Lavalink v4 client library")
                url.set("https://github.com/duncte123/lavalink-client")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://mit-license.org/")
                    }
                }
                developers {
                    developer {
                        id.set("duncte123")
                        name.set("Duncan Sterken")
                        email.set("contact@duncte123.me")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/duncte123/lavalink-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com:duncte123/lavalink-client.git")
                    url.set("https://github.com/duncte123/lavalink-client")
                }
            }

            from(components["java"])

            artifactId = archivesBaseName
            groupId = project.group as String
            version = project.version as String

            artifact(sourcesJar)
        }
    }
}

fun getSuffix(): String {
    if (isSnapshot) {
        return "-SNAPSHOT_${System.getenv("GITHUB_RUN_NUMBER")}"
    }

    return ""
}

val publish: Task by tasks

publish.apply {
    dependsOn(tasks.build)

    onlyIf {
        System.getenv("USERNAME") != null && System.getenv("PASSWORD") != null
    }
}

fun versionFromGit(): Pair<String, Boolean> {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        val headTag = git.tag
            .list()
            .find { it.commit.id == git.head().id }

        val clean = git.status().isClean || System.getenv("CI") != null
        if (!clean) {
            logger.lifecycle("Git state is dirty, version is a snapshot.")
        }

        return if (headTag != null && clean) headTag.name to true else "${git.head().id}-SNAPSHOT" to false
    }
}
