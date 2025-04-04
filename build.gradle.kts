import com.vanniktech.maven.publish.SonatypeHost

plugins {
    java
    kotlin("jvm") version "2.1.20"
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("org.jmailen.kotlinter") version "5.0.1"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jetbrains.kotlinx.atomicfu") version "0.27.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.github.jillesvangurp:kotlin4example:1.1.6")
    testImplementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") } // used by kotlin4example
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()

    finalizedBy("koverHtmlReport")
}

dokka {
    basePublicationsDirectory.set(layout.buildDirectory.dir("docs"))

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs/javadoc"))
    }
}

version = System.getenv("LIBRARY_VERSION") ?: project.findProperty("localLibraryVersion") ?: "-.-.-"

println("Using version: $version")

mavenPublishing {
    coordinates(
        groupId = "io.github.frantoso",
        artifactId = "jasm",
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("Kotlin library for implementing state machines")
        description.set(
            "This library can be used by JVM targets which want to implement state machines",
        )
        inceptionYear.set("2025")
        url.set("https://github.com/frantoso/jasm/")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("frantoso")
                name.set("The frantoso developers")
                url.set("https://github.com/frantoso/jasm/")
            }
        }

        scm {
            url.set("https://github.com/frantoso/jasm/")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}

