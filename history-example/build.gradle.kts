plugins {
    id("jasm.kotlin-conventions")
    application
}

dependencies {
    implementation(project(":jasm"))
}

application {
    mainClass.set("io.github.frantoso.ProgramKt")
}
