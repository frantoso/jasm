plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.3.0")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.4")
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.29.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.1.0")
    implementation("org.jmailen.kotlinter:org.jmailen.kotlinter.gradle.plugin:5.3.0")
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.35.0")
    implementation("com.github.ben-manes.versions:com.github.ben-manes.versions.gradle.plugin:0.53.0")
}

kotlin {
    jvmToolchain(21)
}
