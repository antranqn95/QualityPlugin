plugins {
    kotlin("jvm")
    id("com.company.android-quality")
}

repositories {
    mavenCentral()
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

androidQuality {
    ai {
        apiKey = providers.gradleProperty("ANTHROPIC_API_KEY").orNull
    }
    security {
        enabled = true
        failOnViolation = false
    }
    testGeneration {
        targetPackages = listOf("com.example.sampleapp")
        testFramework = "junit4"
        outputDir = "src/test/java"
        skipExisting = true
    }
}
