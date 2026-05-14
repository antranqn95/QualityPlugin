plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

// Allow JitPack to override group/version via -Pgroup and -Pversion flags
group = findProperty("group")?.toString() ?: "com.company"
version = findProperty("version")?.toString() ?: "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("androidQuality") {
            id = "com.company.android-quality"
            implementationClass = "com.company.androidquality.AndroidQualityPlugin"
        }
    }
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-api:1.23.7")
    implementation("io.gitlab.arturbosch.detekt:detekt-core:1.23.7")
    implementation("io.gitlab.arturbosch.detekt:detekt-parser:1.23.7")
    // Bundled so the plugin can auto-apply Detekt when useDetekt = true
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.7")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            // java-gradle-plugin automatically adds from(components["java"]) to this publication
            // groupId/version come from project.group/version above (overridable by JitPack -Pgroup/-Pversion)
            // artifactId defaults to module name: "plugin"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/antranqn95/QualityPlugin")
            credentials {
                username = providers.gradleProperty("GITHUB_ACTOR").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("GITHUB_TOKEN").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
