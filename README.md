# android-quality-plugin

Gradle plugin for AI-powered test generation and security vulnerability detection across Android projects.

## What it does

- `./gradlew generateTests` — Uses Claude AI to generate JUnit test files for your ViewModels, UseCases, and Repositories
- `./gradlew checkSecurity` — Detects 10 MASVS security vulnerabilities: hardcoded credentials, insecure HTTP, weak crypto, exposed components, and more

## Demo

Run `checkSecurity` on the included `sample-app` to see real violations detected immediately:

```bash
./gradlew :sample-app:checkSecurity
```

Expected output:
```
[AndroidQuality Security] HardcodedCredentials: Hardcoded credential in 'password'...
[AndroidQuality Security] InsecureHttpUsage: Insecure HTTP URL 'http://7go.xyz:8080'...
[AndroidQuality Security] InsecureHttpUsage: Insecure HTTP URL 'http://api.internal...'...
[AndroidQuality Security] InsecureHttpUsage: Insecure HTTP URL 'http://10.0.0.1...'...
[AndroidQuality] Found 4 security violation(s).
BUILD SUCCESSFUL
```

## Setup (3 steps)

**1. Add the plugin repository to `settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/YOUR_ORG/android-quality-plugin")
            credentials {
                username = providers.gradleProperty("GITHUB_ACTOR").get()
                password = providers.gradleProperty("GITHUB_TOKEN").get()
            }
        }
        gradlePluginPortal()
    }
}
```

**2. Apply the plugin in your app `build.gradle.kts`:**

```kotlin
plugins {
    id("com.company.android-quality") version "1.0.0"
}

androidQuality {
    ai {
        apiKey = providers.gradleProperty("AI_API_KEY").orNull
    }
    testGeneration {
        targetPackages = listOf("com.yourapp.viewmodel", "com.yourapp.usecase")
        testFramework = "junit4"  // or "junit5"
        outputDir = "src/test/java"
        skipExisting = true
        useCache = true          // skip API call when source is unchanged
        autoGenOnBuild = false   // set true to run generateTests as part of build
    }
    security {
        enabled = true
        failOnViolation = false  // set true to block builds on violations
        generateReport = false   // set true to emit build/reports/android-quality/security-report.html
        useDetekt = false        // set true to wire rules into ./gradlew detekt (requires Detekt plugin)
    }
}
```

**3. Add credentials to `~/.gradle/gradle.properties`:**

```
AI_API_KEY=your-anthropic-api-key
GITHUB_TOKEN=your-github-token
GITHUB_ACTOR=your-github-username
```

## Tasks

| Task | Command | Description |
|---|---|---|
| Check security | `./gradlew checkSecurity` | Find hardcoded credentials, HTTP URLs, unencrypted storage |
| Generate quality report | `./gradlew generateQualityReport` | Merge security + Detekt findings into a unified report for Claude Code |
| Generate tests | `./gradlew generateTests` | AI-generate JUnit tests for configured packages |

## Security Rules

All rules run automatically as part of `checkSecurity`. Each maps to an [OWASP MASVS](https://mas.owasp.org/MASVS/) control.

| Rule | MASVS | What it detects | Example |
|---|---|---|---|
| `HardcodedCredentials` | MSTG-STORAGE-14 | Credential variable assigned a string literal | `val password = "s3cr3t"` |
| `InsecureHttpUsage` | MSTG-NETWORK-1 | Plain HTTP URL in string | `"http://api.example.com"` |
| `UnencryptedSensitiveStorage` | MSTG-STORAGE-1 | Sensitive key written to plain SharedPreferences | `prefs.putString("token", ...)` |
| `WeakCryptography` | MSTG-CRYPTO-4 | Broken hash/cipher algorithm or ECB mode | `MessageDigest.getInstance("MD5")`, `Cipher.getInstance("AES/ECB/...")` |
| `LogSensitiveData` | MSTG-STORAGE-3 | Password, token, or secret in a log statement | `Log.d(TAG, "token=$token")` |
| `WebViewJavaScriptEnabled` | MSTG-PLATFORM-5 | JavaScript enabled in a WebView | `settings.javaScriptEnabled = true` |
| `InsecureRandom` | MSTG-CRYPTO-6 | `java.util.Random` used instead of `SecureRandom` | `val r = Random()` |
| `ExportedComponent` | MSTG-PLATFORM-1 | `BroadcastReceiver`, `Service`, or `ContentProvider` without permission checks | Class extends `BroadcastReceiver` with no `checkCallingPermission` |
| `DeepLinkInjection` | MSTG-PLATFORM-3 | Unvalidated URI/Intent data passed to a sensitive sink | `webView.loadUrl(uri.getQueryParameter("url"))` |
| `InsecureFileProvider` | MSTG-STORAGE-2 | `Uri.fromFile()` or `Environment.getExternalStorageDirectory()` | `Uri.fromFile(file)` — use `FileProvider` instead |

### Configuring individual rules

Rules can be suppressed per-file with a Detekt baseline or disabled in `detekt.yml`:

```yaml
android-security:
  WeakCryptography:
    active: true
  InsecureRandom:
    active: false  # disable if your project never uses crypto
  ExportedComponent:
    active: true
  DeepLinkInjection:
    active: true
  InsecureFileProvider:
    active: true
```

## Auto-generate tests on build

Set `autoGenOnBuild = true` to have `generateTests` run automatically whenever `./gradlew build` runs:

```kotlin
testGeneration {
    targetPackages = listOf("com.yourapp.viewmodel")
    autoGenOnBuild = true   // runs generateTests as part of build
    useCache = true         // skip API when source unchanged
}
```

## Unified quality report (Claude Code integration)

`generateQualityReport` merges findings from both `checkSecurity` and the Detekt report into a single markdown file optimised for Claude Code:

```bash
# Run security check + generate unified report
./gradlew checkSecurity generateQualityReport

# Or if you use Detekt too — run Detekt first, then generate the combined report
./gradlew detekt checkSecurity generateQualityReport
```

Output: `build/reports/android-quality/quality-report.md` (also `quality-report.json`)

Feed the report directly to Claude Code to fix all issues in one shot:
```bash
claude "read build/reports/android-quality/quality-report.md and fix all issues listed"
```

The report format:
```
## Summary
| Source | Issues |
|--------|--------|
| Security (MASVS) | 7 |
| Detekt | 12 |

## Security Issues (MASVS)
### [SEC-001] HardcodedCredentials — WARNING
- File: `src/main/java/.../DataService.kt:5`
- Fix: Move to gradle.properties or Android Keystore...

## Detekt Issues
### [DET-001] MagicNumber — WARNING
- File: `src/main/java/.../Utils.kt:42`
- Fix: Extract into a named constant...
```

## Detekt integration

The plugin can wire all 10 MASVS security rules into `./gradlew detekt` — so they run alongside your existing Detekt code-quality checks.

**Automatic activation** — just apply both plugins in the same module:

```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.company.android-quality") version "1.0.0"
}
```

Findings appear in the standard Detekt HTML/XML report under the `android-security` rule set.

**Explicit opt-in** (if you want to enable it without Detekt already applied):

```kotlin
security {
    useDetekt = true
}
```

The plugin adds itself to the `detektPlugins` classpath and generates a YAML config snippet that enables all 10 rules on top of your existing `detekt.yml` (using `buildUponDefaultConfig = true`).

## Source directory support

Both `checkSecurity` and `generateTests` automatically scan `src/main/java` **and** `src/main/kotlin` — no extra configuration needed.

## HTML security report

Enable an HTML report that summarizes all findings with severity badges:

```kotlin
security {
    generateReport = true
    reportDir = "build/reports/android-quality"  // default
}
```

Output: `build/reports/android-quality/security-report.html`

## AI response cache

`generateTests` caches a SHA-256 hash of each class's source in `build/android-quality/test-gen-cache.json`. When `useCache = true` (default), the API is not called again if the source hasn't changed since the last run.

## StateFlow / SharedFlow support

When `KotlinCodeAnalyzer` detects `StateFlow`, `MutableStateFlow`, `SharedFlow`, `MutableSharedFlow`, or `Flow<` in a class, the AI prompt is automatically enriched with Turbine-based testing instructions:

```kotlin
// Generated test will include Turbine patterns automatically:
@Test
fun `state is Loading initially`() = runTest {
    viewModel.uiState.test {
        assertEquals(UiState.Loading, awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

Add to test dependencies: `testImplementation("app.cash.turbine:turbine:1.1.0")`

## Android Studio IDE plugin

Build the plugin zip and install it into Android Studio for real-time security highlights:

```bash
./gradlew :ide-plugin:buildPlugin
# Output: ide-plugin/build/distributions/ide-plugin-1.0.0.zip
```

**Install:** Android Studio → Settings → Plugins → Install Plugin from Disk → select the `.zip`

All 10 security rules are registered as IntelliJ inspections under **Android › Security (OWASP MASVS)**. Violations appear as yellow underlines directly in the editor with fix descriptions.

> Note: First build requires downloading the IntelliJ Platform SDK (~1 GB).

## Requirements

- Gradle 8.0+
- JDK 11+
- Anthropic API key (for `generateTests` only)
