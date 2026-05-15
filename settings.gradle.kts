pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.company.android-quality") {
                useModule("com.company:android-quality-plugin:1.0.0")
            }
        }
    }
}

rootProject.name = "android-quality-plugin"
include(":plugin")

// JitPack only needs :plugin — exclude sample-app and ide-plugin to avoid circular dependency
if (System.getenv("JITPACK") == null) {
    include(":sample-app")
    // IDE plugin for Android Studio — requires IntelliJ SDK download (~1 GB).
    // Run: ./gradlew :ide-plugin:buildPlugin   → produces ide-plugin/build/distributions/ide-plugin-1.0.0.zip
    include(":ide-plugin")
}
