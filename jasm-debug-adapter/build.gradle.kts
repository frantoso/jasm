plugins {
    id("jasm.kotlin-conventions")
}

dependencies {
    implementation(project(":jasm"))
    implementation("com.google.code.gson:gson:2.13.2")
}

mavenPublishing {
    coordinates(
        groupId = "io.github.frantoso",
        artifactId = "jasm-debug-adapter",
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("jasm-debug-adapter")
        description.set("Debug adapter for jasm-based state machines")
        inceptionYear.set("2026")
        url.set("https://github.com/frantoso/jasm/")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
