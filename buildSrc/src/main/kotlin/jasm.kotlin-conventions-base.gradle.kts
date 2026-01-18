import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jmailen.kotlinter")
    id("com.github.ben-manes.versions")
}

group = "io.github.frantoso"

version = System.getenv("LIBRARY_VERSION") ?: project.findProperty("localLibraryVersion") ?: "-.-.-"

println("Using version: $version")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("com.github.jillesvangurp:kotlin4example:1.1.9")
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

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
