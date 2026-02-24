@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.ajoberstar.grgit.Grgit
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.kotlin.dsl.register
import java.time.LocalDate

plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    id("org.jetbrains.dokka-javadoc") version libs.versions.dokka
    id("org.ajoberstar.grgit") version libs.versions.grgit
    id("com.vanniktech.maven.publish.base") version libs.versions.maven.publish
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${libs.versions.dokka.get()}")
    }
}

val (gitVersion, release) = versionFromGit()
logger.lifecycle("Version: $gitVersion (release: $release)")

group = "dev.arbjerg"
version = gitVersion
val archivesBaseName = "lavalink-client"

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.lavalink.dev/releases")
        maven("https://maven.lavalink.dev/snapshots")
        maven("https://maven.topi.wtf/releases")
        // Note to self: jitpack always comes last
        maven("https://jitpack.io")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // package libraries
    api(kotlin("stdlib"))
//    api(libs.kotlin)
    api(libs.lavalink.protocol)
    api(libs.okhttp)
    api(libs.jackson)
    api(libs.bundles.reactor)
    api(libs.logger.api)

    // Discord library support
    compileOnly(libs.jda)
    compileOnly(libs.d4j)

    testImplementation(kotlin("test"))
    testImplementation(libs.jda)
    testImplementation(libs.d4j)
    testImplementation(libs.logger.impl)
    testImplementation(libs.lyrics)
    testImplementation(libs.lavasearch)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesForRelease = tasks.register<Copy>("sourcesForRelease") {
    from("src/main/kotlin") {
        include("**/LLClientInfo.kt.txt")

        filter<ReplaceTokens>(
            mapOf(
                "tokens" to mapOf(
                    "VERSION" to project.version
                )
            )
        )

        rename("LLClientInfo.kt.txt", "LLClientInfo.kt")
    }
    into("build/filteredSrc")

    includeEmptyDirs = false
}

val generateKotlinSources = tasks.register<SourceTask>("generateKotlinSources") {
    val javaSources = sourceSets["main"].allSource.filter {
        it.name != "LLClientInfo.kt"
    }.asFileTree

    source = javaSources + fileTree(sourcesForRelease.get().destinationDir)
    dependsOn(sourcesForRelease)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.compileKotlin {
    source(generateKotlinSources.get().source)
    dependsOn(generateKotlinSources)
}

tasks.build {
    dependsOn(tasks.dokkaGenerate)
}

tasks.test {
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "9.3.1"
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

tasks.withType<GenerateModuleMetadata> {
    dependsOn(sourcesJar)
    dependsOn(tasks.kotlinSourcesJar)
}

kotlin {
    jvmToolchain(17)
}

dokka {
    pluginsConfiguration.html {
        customAssets.from(listOf(file("dokka/assets/logo-icon.svg")))
        footerMessage.set("&copy; ${LocalDate.now().year} Lavalink devs<br />Licensed under the MIT license")
        separateInheritedMembers.set(true)
    }
}

mavenPublishing {
    configure(KotlinJvm(
        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
        sourcesJar = true
    ))
}

afterEvaluate {
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<PublishingExtension> {
            val mavenUsername = findProperty("MAVEN_USERNAME") as String?
            val mavenPassword = findProperty("MAVEN_PASSWORD") as String?
            if (!mavenUsername.isNullOrEmpty() && !mavenPassword.isNullOrEmpty()) {
                repositories {
                    val snapshots = "https://maven.lavalink.dev/snapshots"
                    val releases = "https://maven.lavalink.dev/releases"

                    maven(if (release) releases else snapshots) {
                        credentials {
                            username = mavenUsername
                            password = mavenPassword
                        }
                    }
                }
            } else {
                logger.lifecycle("Not publishing to maven.lavalink.dev because credentials are not set")
            }
        }

        configure<MavenPublishBaseExtension> {
            coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())
            val mavenCentralUsername = findProperty("mavenCentralUsername") as String?
            val mavenCentralPassword = findProperty("mavenCentralPassword") as String?
            if (!mavenCentralUsername.isNullOrEmpty() && !mavenCentralPassword.isNullOrEmpty()) {
                publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, false)
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
