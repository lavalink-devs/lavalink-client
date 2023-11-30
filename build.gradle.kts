@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.apache.tools.ant.filters.ReplaceTokens
import org.ajoberstar.grgit.Grgit

plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    id("org.ajoberstar.grgit") version libs.versions.grgit
    id("com.vanniktech.maven.publish.base") version libs.versions.maven.publish
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
    maven("https://maven.topi.wtf/releases")
    // Note to self: jitpack always comes last
    maven("https://jitpack.io")
}

dependencies {
    // package libraries
    api(kotlin("stdlib"))
    api(libs.kotlin)
    api(libs.lavalink.protocol)
    api(libs.okhttp)
    api(libs.bundles.reactor)
    api(libs.logger.api)

    // Discord library support
    compileOnly(libs.jda)
    compileOnly(libs.d4j)

    testImplementation(kotlin("test"))
    testImplementation(libs.jda)
    testImplementation(libs.d4j)
    testImplementation(libs.logger.impl)
    testImplementation(libs.lavasearch)
}

val sourcesForRelease = task<Copy>("sourcesForRelease") {
    from("src/main/kotlin") {
        include("**/LLClientInfo.java")

        filter<ReplaceTokens>(mapOf("tokens" to mapOf(
            "VERSION" to project.version
        )))
    }
    into("build/filteredSrc")

    includeEmptyDirs = false
}

val generateKotlinSources = task<SourceTask>("generateKotlinSources") {
    val javaSources = sourceSets["main"].allSource.filter {
        it.name != "LLClientInfo.java"
    }.asFileTree

    source = javaSources + fileTree(sourcesForRelease.destinationDir)
    dependsOn(sourcesForRelease)
}

val sourcesJar = task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.compileJava {
    source = generateKotlinSources.source
    dependsOn(generateKotlinSources)
}

tasks.build {
    dependsOn(tasks.dokkaJavadoc)
}

tasks.test {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "8.1.1"
    distributionType = Wrapper.DistributionType.BIN
}

tasks.withType<Sign> {
    dependsOn(sourcesJar)
    dependsOn(tasks.kotlinSourcesJar)
}

tasks.withType<PublishToMavenRepository> {
    dependsOn(sourcesJar)
    dependsOn(tasks.kotlinSourcesJar)
}

kotlin {
    jvmToolchain(17)
}

val mavenUrl: String
    get() {
        if (release) {
            return "https://maven.lavalink.dev/releases"
        }

        return "https://maven.lavalink.dev/snapshots"
    }

publishing {
    repositories {
        maven {
            name = "arbjerg"
            url = uri(mavenUrl)
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

mavenPublishing {
    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaJavadoc"),
        sourcesJar = true
    ))
}

afterEvaluate {
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<MavenPublishBaseExtension> {
            coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())

            if (findProperty("mavenCentralUsername") != null && findProperty("mavenCentralPassword") != null) {
                publishToMavenCentral(SonatypeHost.S01, false)
                if (release) {
                    signAllPublications()
                }
            } else {
                logger.lifecycle("Not publishing to OSSRH due to missing credentials")
            }

            pom {
                name.set(archivesBaseName)
                description.set("Lavalink v4 client library")
                url.set("https://github.com/lavalink-devs/lavalink-client")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/lavalink-devs/lavalink-client/blob/main/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("duncte123")
                        name.set("Duncan Sterken")
                        url.set("https://duncte123.dev/")
                    }
                }

                scm {
                    url.set("https://github.com/lavalink-devs/lavalink-client")
                    connection.set("scm:git:git://github.com/lavalink-devs/lavalink-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com:lavalink-devs/lavalink-client.git")
                }
            }
        }
    }
}

val publish: Task by tasks

publish.apply {
    dependsOn(tasks.build)

    onlyIf {
        System.getenv("MAVEN_USERNAME") != null && System.getenv("MAVEN_PASSWORD") != null
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
