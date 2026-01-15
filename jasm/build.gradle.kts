plugins {
    id("jasm.kotlin-conventions")
}

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
    publishToMavenCentral()

    // Enable GPG signing for all publications
    signAllPublications()
}
