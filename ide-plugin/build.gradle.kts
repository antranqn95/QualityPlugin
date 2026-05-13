plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.company"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3.6")
    type.set("IC")
    plugins.set(listOf("org.jetbrains.kotlin"))
    downloadSources.set(false)
    updateSinceUntilBuild.set(false)
}

tasks.patchPluginXml {
    sinceBuild.set("233")
    untilBuild.set("243.*")
}

// Disable searchable options indexing — not needed and slow in CI
tasks.buildSearchableOptions {
    enabled = false
}
