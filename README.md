# android-quality-plugin

Gradle plugin for AI-powered test generation and MASVS security vulnerability detection for Android projects.

## What it does

- `./gradlew checkSecurity` — Detects 10 MASVS security vulnerabilities: hardcoded credentials, insecure HTTP, weak crypto, exposed components, and more
- `./gradlew generateQualityReport` — Merges security + Detekt findings into a unified Markdown report for Claude Code to fix
- `./gradlew generateTests` — Uses Claude AI to generate JUnit test files for ViewModels, UseCases, and Repositories

## Demo

Run `checkSecurity` on the included `sample-app`:

```bash
./gradlew :sample-app:checkSecurity
```

Expected output:
```
[AndroidQuality Security] HardcodedCredentials: Hardcoded credential in 'password'...
[AndroidQuality Security] InsecureHttpUsage: Insecure HTTP URL 'http://7go.xyz:8080'...
[AndroidQuality] Found 7 security violation(s).
BUILD SUCCESSFUL
```

---

## Setup — Groovy DSL (`build.gradle`)

### 1. `settings.gradle`

```groovy
pluginManagement {
    repositories {
        maven { url 'https://jitpack.io' }   // no credentials needed
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'com.company.android-quality') {
                useModule('com.github.antranqn95.QualityPlugin:plugin:1.0.3')
            }
        }
    }
}
```

### 2. `app/build.gradle`

```groovy
plugins {
    id 'com.android.application'
    // ... other plugins
    id 'com.company.android-quality' version '1.0.3'
}

androidQuality {
    security {
        enabled = true
        failOnViolation = false   // set true to block build on violations
        generateReport = true     // emit security-report.html
        useDetekt = false         // set true to also wire MASVS rules into ./gradlew detekt
    }
}
```

### 3. Optional — Detekt + Compose rules integration

Set `useDetekt = true` — the plugin **automatically applies Detekt** and wires all 10 MASVS rules into `./gradlew detekt`. No extra plugin declaration needed.

Add `useComposeRules = true` for Jetpack Compose projects — automatically adds [compose-rules](https://github.com/mrmans0n/compose-rules) (0.4.28) to `detektPlugins`.

```groovy
androidQuality {
    security {
        enabled = true
        useDetekt = true          // auto-applies Detekt + wires MASVS rules
        useComposeRules = true    // adds Compose best-practice rules (requires Compose project)
    }
}
```

### 4. Optional — AI test generation

Add your Anthropic API key to `~/.gradle/gradle.properties`:

```properties
ANTHROPIC_API_KEY=sk-ant-...
```

Then configure in `build.gradle`:

```groovy
androidQuality {
    ai {
        apiKey = findProperty('ANTHROPIC_API_KEY')
    }
    testGeneration {
        targetPackages = ['com.yourapp.viewmodel', 'com.yourapp.usecase']
        testFramework = 'junit4'    // or 'junit5'
        outputDir = 'src/test/java'
        skipExisting = true
        useCache = true             // skip API if source unchanged
        autoGenOnBuild = false      // set true to auto-run with ./gradlew build
    }
}
```

---

## Setup — Kotlin DSL (`build.gradle.kts`)

### 1. `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }   // no credentials needed
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.company.android-quality") {
                useModule("com.github.antranqn95.QualityPlugin:plugin:1.0.3")
            }
        }
    }
}
```

### 2. `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    // ... other plugins
    id("com.company.android-quality") version "1.0.3"
}

androidQuality {
    security {
        enabled = true
        failOnViolation = false   // set true to block build on violations
        generateReport = true     // emit security-report.html
        useDetekt = false         // set true to also wire MASVS rules into ./gradlew detekt
    }
}
```

---

## Tasks

| Task | Command | Description |
|---|---|---|
| Check security | `./gradlew :app:checkSecurity` | Find hardcoded credentials, HTTP URLs, unencrypted storage |
| Quality report | `./gradlew :app:generateQualityReport` | Merge security + Detekt findings into one report |
| Generate tests | `./gradlew :app:generateTests` | AI-generate JUnit tests for configured packages |

---

## Security Rules

All 10 rules run automatically as part of `checkSecurity`. Each maps to an [OWASP MASVS](https://mas.owasp.org/MASVS/) control.

| Rule | MASVS | What it detects | Example |
|---|---|---|---|
| `HardcodedCredentials` | MSTG-STORAGE-14 | Credential variable assigned a string literal | `val password = "s3cr3t"` |
| `InsecureHttpUsage` | MSTG-NETWORK-1 | Plain HTTP URL in string | `"http://api.example.com"` |
| `UnencryptedSensitiveStorage` | MSTG-STORAGE-1 | Sensitive key written to plain SharedPreferences | `prefs.putString("token", ...)` |
| `WeakCryptography` | MSTG-CRYPTO-4 | Broken hash/cipher algorithm or ECB mode | `MessageDigest.getInstance("MD5")` |
| `LogSensitiveData` | MSTG-STORAGE-3 | Password, token, or secret in a log statement | `Log.d(TAG, "token=$token")` |
| `WebViewJavaScriptEnabled` | MSTG-PLATFORM-5 | JavaScript enabled in a WebView | `settings.javaScriptEnabled = true` |
| `InsecureRandom` | MSTG-CRYPTO-6 | `java.util.Random` used instead of `SecureRandom` | `val r = Random()` |
| `ExportedComponent` | MSTG-PLATFORM-1 | `BroadcastReceiver`/`Service`/`ContentProvider` without permission checks | Extends `BroadcastReceiver` with no `checkCallingPermission` |
| `DeepLinkInjection` | MSTG-PLATFORM-3 | Unvalidated URI/Intent data passed to a sensitive sink | `webView.loadUrl(uri.getQueryParameter("url"))` |
| `InsecureFileProvider` | MSTG-STORAGE-2 | `Uri.fromFile()` or `Environment.getExternalStorageDirectory()` | `Uri.fromFile(file)` — use `FileProvider` instead |

### Disable individual rules in `detekt.yml`

```yaml
android-security:
  WeakCryptography:
    active: true
  InsecureRandom:
    active: false   # disable if not applicable
  ExportedComponent:
    active: true
```

---

## Unified quality report (Claude Code integration)

`generateQualityReport` runs `checkSecurity` automatically, then merges all findings into a single Markdown file. If `useDetekt = true` and Detekt has already been run, its findings are included as well.

```bash
# Run security check + generate report (one command)
./gradlew :app:generateQualityReport

# With Detekt findings included (requires useDetekt = true)
./gradlew :app:detekt :app:generateQualityReport
```

Output: `app/build/reports/android-quality/quality-report.md`

Fix all issues with Claude Code:
```bash
claude "read app/build/reports/android-quality/quality-report.md and fix all issues listed"
```

---

## Detekt integration (optional)

Set `useDetekt = true` — no additional plugin declaration required. The Android Quality plugin **auto-applies Detekt** and wires all 10 MASVS rules into `./gradlew detekt`.

```groovy
androidQuality {
    security {
        useDetekt = true
        useComposeRules = true   // optional: adds Compose best-practice rules
    }
}
```

When enabled, findings appear in the standard Detekt HTML/XML report under the `android-security` rule set. Override per-rule in your `detekt.yml`.

> Without `useDetekt = true`, `./gradlew detekt` will not include MASVS security findings. Use `./gradlew checkSecurity` or `./gradlew generateQualityReport` instead — these run standalone with no Detekt dependency.

## Compose rules (optional)

When `useDetekt = true` and `useComposeRules = true`, the plugin automatically adds [compose-rules 0.4.28](https://github.com/mrmans0n/compose-rules) to `detektPlugins`. No manual dependency needed.

Rules enforced (~30+):

| Category | Examples |
|---|---|
| State | `mutableStateOf` must be in `remember()`, use `mutableIntStateOf` for primitives |
| Composables | No ViewModel as parameter, naming convention, single content emission |
| Modifiers | Public composables must expose `modifier: Modifier = Modifier`, no modifier reuse |
| Parameters | Order: required → Modifier → optional → trailing lambda; callbacks named `onX` |
| Preview | Preview-only composables must be `private` |

---

## HTML security report

```groovy
security {
    generateReport = true
    reportDir = "build/reports/android-quality"  // default
}
```

Output: `build/reports/android-quality/security-report.html`

---

## AI response cache

`generateTests` caches a SHA-256 hash of each class's source in `build/android-quality/test-gen-cache.json`. When `useCache = true` (default), the API is skipped if the source has not changed since the last run.

---

## StateFlow / SharedFlow support

When `KotlinCodeAnalyzer` detects Flow types in a class, the AI prompt automatically includes Turbine-based testing patterns:

```kotlin
@Test
fun `state is Loading initially`() = runTest {
    viewModel.uiState.test {
        assertEquals(UiState.Loading, awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

Add to test dependencies: `testImplementation("app.cash.turbine:turbine:1.1.0")`

---

## Android Studio IDE plugin

Install the IDE plugin for real-time security highlights directly in the editor:

```bash
./gradlew :ide-plugin:buildPlugin
# Output: ide-plugin/build/distributions/ide-plugin-1.0.0.zip
```

**Install:** Android Studio → Settings → Plugins → Install Plugin from Disk → select the `.zip`

All 10 rules appear as IntelliJ inspections under **Android › Security (OWASP MASVS)** — violations show as yellow underlines with fix descriptions.

> Note: First build requires downloading the IntelliJ Platform SDK (~1 GB).

---

## Requirements

- Gradle 8.0+
- JDK 11+
- Anthropic API key (for `generateTests` only)
