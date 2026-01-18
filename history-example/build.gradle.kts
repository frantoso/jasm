plugins {
    id("jasm.kotlin-conventions-base")
    application
}

dependencies {
    implementation(project(":jasm"))
    implementation(project(":jasm-debug-adapter"))
}

application {
    mainClass.set("io.github.frantoso.jasm.ProgramKt")
}
