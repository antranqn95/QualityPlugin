# Android Quality Plugin — Technical Document

> **Version:** 1.2.0 | **Updated:** 2026-05-13 | **Author:** AnTran

---

## 1. Tổng quan

**Android Quality Plugin** là một Gradle plugin tự phát triển (custom Gradle plugin) nhằm tự động hóa hai quy trình quan trọng trong vòng đời phát triển Android:

1. **Static Security Analysis** — Phát hiện lỗ hổng bảo mật trong source code Kotlin tại thời điểm build thông qua bộ quy tắc Detekt tùy chỉnh, liên kết trực tiếp với chuẩn OWASP MASVS.
2. **AI-Powered Test Generation** — Tự động sinh JUnit test files cho các class Kotlin (ViewModel, UseCase, Repository) bằng cách gọi Anthropic Claude API.

Plugin được đóng gói và phân phối qua Maven, tích hợp vào dự án Android chỉ bằng vài dòng cấu hình trong `build.gradle.kts`.

---

## 2. Kiến trúc tổng thể

```
android-quality-plugin/
├── plugin/                          # Core plugin module
│   ├── src/main/kotlin/
│   │   ├── AndroidQualityPlugin.kt  # Entry point — đăng ký task & extension
│   │   ├── extension/               # DSL configuration classes
│   │   │   ├── AndroidQualityExtension.kt
│   │   │   ├── AiConfig.kt
│   │   │   ├── SecurityConfig.kt
│   │   │   └── TestGenerationConfig.kt
│   │   ├── detekt/security/         # 10 custom Detekt rules
│   │   │   ├── SecurityRuleSetProvider.kt
│   │   │   ├── HardcodedCredentialsRule.kt
│   │   │   ├── InsecureHttpUsageRule.kt
│   │   │   ├── InsecureRandomRule.kt
│   │   │   ├── LogSensitiveDataRule.kt
│   │   │   ├── UnencryptedSensitiveStorageRule.kt
│   │   │   ├── WeakCryptographyRule.kt
│   │   │   ├── WebViewJavaScriptEnabledRule.kt
│   │   │   ├── ExportedComponentRule.kt
│   │   │   ├── DeepLinkInjectionRule.kt
│   │   │   └── InsecureFileProviderRule.kt
│   │   ├── detekt/integration/      # Detekt plugin wiring
│   │   │   └── DetektIntegration.kt # Auto-wire rules into ./gradlew detekt
│   │   ├── tasks/
│   │   │   ├── CheckSecurityTask.kt # Chạy tất cả 10 rules + HTML report
│   │   │   └── GenerateTestsTask.kt # Gọi AI để sinh test + cache
│   │   ├── report/
│   │   │   └── SecurityReportGenerator.kt  # NEW — HTML report
│   │   └── ai/
│   │       ├── AITestGenerator.kt   # HTTP client → Anthropic API
│   │       ├── KotlinCodeAnalyzer.kt# Parse .kt files → ClassInfo
│   │       └── TestGenCache.kt      # NEW — SHA-256 source cache
│   └── src/test/kotlin/             # 127 unit & integration tests
└── ide-plugin/                      # IntelliJ/Android Studio plugin
│   └── src/main/kotlin/.../ide/inspections/  # 10 LocalInspectionTool
│   └── src/main/resources/META-INF/plugin.xml
└── sample-app/                      # Demo project
```

### 2.1 Luồng hoạt động — `checkSecurity`

```
./gradlew checkSecurity
        │
        ▼
CheckSecurityTask.check()
        │
        ├─ Scan src/main/java/**/*.kt + src/main/kotlin/**/*.kt
        │
        ├─ Compile với KtCompiler (Detekt parser)
        │
        ├─ Chạy 10 rules song song trên mỗi file
        │       ├── HardcodedCredentialsRule
        │       ├── InsecureHttpUsageRule
        │       ├── InsecureRandomRule
        │       ├── LogSensitiveDataRule
        │       ├── UnencryptedSensitiveStorageRule
        │       ├── WeakCryptographyRule
        │       ├── WebViewJavaScriptEnabledRule
        │       ├── ExportedComponentRule        (NEW)
        │       ├── DeepLinkInjectionRule        (NEW)
        │       └── InsecureFileProviderRule     (NEW)
        │
        ├─ findings.isEmpty()
        │      ├── true  → BUILD SUCCESS
        │      └── false → log warnings
        │                → failOnViolation=true → GradleException (BUILD FAILED)
        │
        └─ generateReport=true → SecurityReportGenerator → security-report.html
```

### 2.2 Luồng hoạt động — `generateTests`

```
./gradlew generateTests
        │
        ▼
GenerateTestsTask.generate()
        │
        ├─ Kiểm tra apiKey (skip nếu null/blank)
        │
        ├─ KotlinCodeAnalyzer.analyze(targetPackages)
        │       └─ Parse từng .kt file → ClassInfo(className, packageName, source)
        │
        ├─ Với mỗi class:
        │       ├─ skipExisting=true & test đã tồn tại → skip
        │       ├─ useCache=true & TestGenCache.isUpToDate() → skip (source chưa đổi)
        │       └─ AITestGenerator.generate(classInfo, testFramework)
        │               ├─ Build prompt (source code + yêu cầu)
        │               ├─ POST → https://api.anthropic.com/v1/messages
        │               ├─ Retry nếu 429 (rate limit)
        │               ├─ Parse JSON response
        │               └─ stripMarkdownFences() → clean Kotlin code
        │
        ├─ Write {ClassName}Test.kt → outputDir/{packagePath}/
        └─ TestGenCache.put(className, sha256(source)) → build/android-quality/test-gen-cache.json
```

---

## 3. Chi tiết các tính năng

### 3.1 Security Rules

Mỗi rule kế thừa từ `Rule` (Detekt API), sử dụng **Visitor pattern** để duyệt AST của file Kotlin.

| Rule | MASVS | Phát hiện | Ví dụ vi phạm |
|------|-------|-----------|---------------|
| `HardcodedCredentials` | MSTG-STORAGE-2 | Variable name chứa `password`, `apiKey`, `token`, `secret` có string literal không rỗng | `val password = "hunter2"` |
| `InsecureHttpUsage` | MSTG-NETWORK-3 | String literal bắt đầu bằng `http://` (không phải `https://`) | `val url = "http://api.example.com"` |
| `WeakCryptography` | MSTG-CRYPTO-4 | `MessageDigest.getInstance("MD5"/"SHA-1")`, `Cipher.getInstance("DES"/"AES/ECB/..."/"RC4")` | `Cipher.getInstance("AES/ECB/PKCS5Padding")` |
| `LogSensitiveData` | MSTG-STORAGE-3 | `Log.d/e/i/w/v` hoặc `println` có message chứa keyword nhạy cảm | `Log.d(TAG, "password=$password")` |
| `UnencryptedSensitiveStorage` | MSTG-STORAGE-1 | `SharedPreferences.putString/getString` với key chứa `password`, `token`, `secret` | `prefs.edit().putString("auth_token", value)` |
| `WebViewJavaScriptEnabled` | MSTG-PLATFORM-5 | `webView.settings.javaScriptEnabled = true` | `settings.javaScriptEnabled = true` |
| `InsecureRandom` | MSTG-CRYPTO-6 | Sử dụng `Random()` hoặc `java.util.Random()` thay vì `SecureRandom` | `val r = Random()` |
| `ExportedComponent` | MSTG-PLATFORM-1 | `BroadcastReceiver`, `Service`, hoặc `ContentProvider` không có `checkCallingPermission` / `enforcePermission` | Class extends `BroadcastReceiver` không có permission check |
| `DeepLinkInjection` | MSTG-PLATFORM-3 | Giá trị từ `getQueryParameter`, `getStringExtra`, `getData` truyền thẳng vào `loadUrl`, `startActivity`, `sendBroadcast` mà không sanitize | `webView.loadUrl(uri.getQueryParameter("redirect"))` |
| `InsecureFileProvider` | MSTG-STORAGE-2 | `Uri.fromFile()`, `Environment.getExternalStorageDirectory()`, `getExternalStoragePublicDirectory()` | `Uri.fromFile(file)` — nên dùng `FileProvider.getUriForFile()` |

**Cấu hình:**
```kotlin
androidQuality {
    security {
        enabled = true           // bật/tắt toàn bộ check
        failOnViolation = false  // true = BUILD FAILED khi có vi phạm
        generateReport = false   // true → xuất HTML report
        reportDir = "build/reports/android-quality"
        useDetekt = false        // true → wire rules vào ./gradlew detekt (cần Detekt plugin)
    }
}
```

### 3.2 AI Test Generation

`KotlinCodeAnalyzer` dùng Regex để extract class đầu tiên (non-private, non-interface) từ mỗi file `.kt`. Thông tin được đóng gói thành `ClassInfo` gồm `className`, `packageName`, `packagePath`, `source`.

`AITestGenerator` gửi source code của class lên Claude API kèm prompt yêu cầu sinh test với:
- JUnit 4/5 annotations
- MockK cho mocking
- `runTest` cho coroutines
- Test names dạng backtick
- Happy path + error cases

**Response parsing** lấy `content[0].text` từ JSON response của Anthropic, sau đó `stripMarkdownFences()` loại bỏ ` ```kotlin...``` ` nếu model trả về có wrapper.

**Cấu hình:**
```kotlin
androidQuality {
    ai {
        apiKey = "sk-ant-..."          // Anthropic API key
        model  = "claude-sonnet-4-6"  // model mặc định
    }
    testGeneration {
        targetPackages = listOf("com.example.app.viewmodel", "com.example.app.usecase")
        testFramework  = "junit4"      // hoặc "junit5"
        outputDir      = "src/test/java"
        skipExisting   = true          // không ghi đè test đã có
        useCache       = true          // bỏ qua API nếu source chưa đổi (SHA-256 cache)
    }
}
```

---

## 4. Stack công nghệ

### 4.1 Core

| Công nghệ | Version | Vai trò |
|-----------|---------|---------|
| **Kotlin** | 2.0.0 | Ngôn ngữ chính |
| **Gradle** | 8.5 | Build system + Plugin API |
| **kotlin-dsl** | — | DSL cho `build.gradle.kts` |
| **java-gradle-plugin** | — | Đóng gói Gradle plugin |

### 4.2 Static Analysis

| Công nghệ | Version | Vai trò |
|-----------|---------|---------|
| **Detekt** | 1.23.7 | Framework phân tích tĩnh Kotlin |
| `detekt-api` | 1.23.7 | Base class `Rule`, `RuleSet`, `Finding` |
| `detekt-parser` | 1.23.7 | `KtCompiler` — parse `.kt` → PSI/AST |

### 4.3 AI Integration

| Công nghệ | Vai trò |
|-----------|---------|
| **Java 11 HttpClient** | Gọi Anthropic REST API (không dùng thư viện ngoài) |
| **Anthropic Claude API** | Model `claude-sonnet-4-6` sinh test code |
| **Groovy JsonSlurper / JsonOutput** | Parse và build JSON request/response (có sẵn trong Gradle runtime) |

### 4.4 Testing

| Công nghệ | Version | Vai trò |
|-----------|---------|---------|
| **JUnit 5 (Jupiter)** | 5.10.2 | Test framework cho plugin tests |
| **AssertJ** | 3.25.3 | Fluent assertions |
| **MockK** | 1.13.10 | Kotlin-idiomatic mocking |
| **Gradle TestKit** | — | Integration test plugin thông qua `GradleRunner` |
| **JUnit 4** | 4.13.2 | Test framework cho sample-app |

---

## 5. Lý do lựa chọn công nghệ

### 5.1 Tại sao Detekt thay vì Android Lint hoặc SonarQube?

| Tiêu chí | Detekt | Android Lint | SonarQube |
|---------|--------|-------------|-----------|
| Kotlin-native | ✅ | ⚠️ (Java-based) | ⚠️ |
| Custom rule dễ viết | ✅ Kotlin | ❌ Java + XML | ❌ Phức tạp |
| Chạy offline | ✅ | ✅ | ❌ Cần server |
| Tích hợp Gradle | ✅ Native | ✅ | ⚠️ Plugin riêng |
| AST access | ✅ PSI full | ⚠️ Hạn chế | ⚠️ |
| Cost | Free | Free | Có bản trả phí |

**Kết luận:** Detekt cho phép viết rule bằng Kotlin thuần với full access vào PSI (Program Structure Interface) AST — phù hợp nhất cho custom security rule targeting Kotlin codebase.

### 5.2 Tại sao Claude (Anthropic) thay vì GPT-4 hoặc Gemini?

- **Claude** được tối ưu cho code generation với context window lớn (200K tokens) — đọc được toàn bộ file source phức tạp.
- Claude có tỉ lệ sinh test "compile ngay không cần sửa" cao hơn GPT-4 trong benchmark nội bộ Anthropic cho Kotlin.
- API giá thành cạnh tranh ở tier `claude-sonnet-4-6`.
- Prompt-following tốt: tuân thủ yêu cầu "không dùng markdown wrapper, chỉ trả Kotlin thuần".

### 5.3 Tại sao Java 11 HttpClient thay vì OkHttp/Retrofit?

Plugin chạy trong **Gradle build process** — không phải Android app. Việc thêm OkHttp/Retrofit sẽ:
- Tăng kích thước plugin classpath
- Gây conflict với dependencies của project đang build
- Phức tạp hóa không cần thiết cho một endpoint duy nhất

Java 11 `HttpClient` là built-in, zero-dependency, đủ mạnh cho use case này.

### 5.4 Tại sao Gradle Plugin thay vì standalone CLI tool?

| Tiêu chí | Gradle Plugin | CLI Tool |
|---------|--------------|---------|
| Tích hợp CI/CD | ✅ Tự động trong build | ❌ Cần cấu hình riêng |
| Developer experience | ✅ Chạy từ IDE | ❌ Phải mở terminal riêng |
| Cấu hình trong project | ✅ `build.gradle.kts` | ❌ File config riêng |
| Kế thừa task lifecycle | ✅ `check`, `test` | ❌ |
| Phân phối | ✅ Maven | ❌ Binary distribution |

---

## 6. Design Patterns sử dụng

| Pattern | Áp dụng ở đâu |
|---------|--------------|
| **Visitor** | Detekt Rule duyệt AST — mỗi node type có method `visitXxx()` riêng |
| **Extension DSL** | `androidQuality { security { ... } ai { ... } }` — Gradle extension pattern |
| **Factory Method** | `SecurityRuleSetProvider.instance()` tạo toàn bộ rule set |
| **Strategy** | `AITestGenerator` nhận `testFramework` để build prompt khác nhau |
| **Template Method** | `GenerateTestsTask` định nghĩa flow, delegate từng bước sang `KotlinCodeAnalyzer` và `AITestGenerator` |
| **Dependency Injection** | `AITestGenerator(apiKey, model, client)` — inject `HttpClient` để testable |

---

## 7. Testing Strategy

### Coverage hiện tại

| Module | Test count | Strategy |
|--------|-----------|---------|
| Detekt Rules (10 rules) | 54 tests | Unit test với `detekt-test` `rule.lint(code)` |
| `SecurityRuleSetProvider` | 14 tests | Unit — verify 10 rules registered |
| `KotlinCodeAnalyzer` | 21 tests | Unit — filesystem với `@TempDir` (bao gồm Flow detection) |
| `AITestGenerator` | 16 tests | Unit — MockK mock `HttpClient` (bao gồm Flow prompt) |
| `TestGenCache` | 7 tests | Unit — filesystem với `@TempDir` |
| `CheckSecurityTask` | 11 tests | Integration — `GradleRunner` (real Gradle build) |
| `GenerateTestsTask` | 5 tests | Integration — `GradleRunner` |
| **Tổng plugin** | **127 tests** | |
| Sample-app | 299 tests | Unit — JUnit4 + MockK |

### Phân tầng test

```
Integration Tests (GradleRunner)   ← Chạy real Gradle build, verify end-to-end
        ▲
Unit Tests (MockK + AssertJ)       ← Isolate từng class, fast feedback
        ▲
Rule Tests (detekt-test)           ← Lint code snippets trực tiếp, không cần compile
```

---

## 8. Tích hợp vào dự án Android

**Bước 1:** Thêm vào `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.pkg.github.com/antranqn95/QualityPlugin") }
        gradlePluginPortal()
    }
}
```

**Bước 2:** Áp dụng plugin trong module `build.gradle.kts`:
```kotlin
plugins {
    id("com.company.android-quality") version "1.0.0"
}

androidQuality {
    security {
        enabled = true
        failOnViolation = true  // Chặn build nếu có lỗ hổng
    }
    ai {
        apiKey = providers.gradleProperty("ANTHROPIC_API_KEY").orNull
    }
    testGeneration {
        targetPackages = listOf(
            "com.example.app.viewmodel",
            "com.example.app.usecase",
            "com.example.app.repository"
        )
        skipExisting = true
    }
}
```

**Bước 3:** Chạy:
```bash
./gradlew checkSecurity    # Kiểm tra bảo mật
./gradlew generateTests    # Sinh test tự động
```

---

## 9. CI/CD Integration

Plugin có sẵn GitHub Actions workflow để tự động publish lên GitHub Packages:

```yaml
# .github/workflows/publish.yml
on:
  push:
    branches: [main]
jobs:
  publish:
    steps:
      - run: ./gradlew publish
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Khuyến nghị thêm `checkSecurity` vào CI pipeline:
```yaml
- run: ./gradlew checkSecurity
  # Build sẽ fail nếu phát hiện security violation (failOnViolation=true)
```

---

## 10. Roadmap

### Đã hoàn thành (v1.1.0)

| Tính năng | Mô tả |
|----------|-------|
| `ExportedComponentRule` | Detect Android component không có permission check |
| `DeepLinkInjectionRule` | Detect deep link/intent data truyền vào sink không sanitize |
| `InsecureFileProviderRule` | Detect `Uri.fromFile()` và external storage trực tiếp |
| `src/main/kotlin` support | Scan cả hai source dir tự động |
| HTML security report | `SecurityReportGenerator` xuất file HTML với severity badges |
| AI response cache | `TestGenCache` dùng SHA-256 — bỏ qua API nếu source chưa đổi |

### Đã hoàn thành (v1.2.0)

| Tính năng | Mô tả |
|----------|-------|
| IDE Plugin | 10 `LocalInspectionTool` cho IntelliJ/Android Studio — highlight real-time trong editor |
| StateFlow/SharedFlow | `KotlinCodeAnalyzer` detect Flow types → AI prompt tự động thêm hướng dẫn Turbine |
| Detekt integration | Auto-wire 10 MASVS rules vào `./gradlew detekt` khi cả hai plugin cùng applied |

### Cách dùng IDE Plugin

Build plugin `.zip`:
```bash
./gradlew :ide-plugin:buildPlugin
# Output: ide-plugin/build/distributions/ide-plugin-1.0.0.zip
```

Cài vào Android Studio: **Settings → Plugins → Install Plugin from Disk → chọn file `.zip`**

### Detekt Integration

Khi project apply cả `io.gitlab.arturbosch.detekt` và `com.company.android-quality`, plugin tự động:

1. **Thêm JAR plugin** vào `detektPlugins` configuration (ServiceLoader tự discover `SecurityRuleSetProvider`)
2. **Generate YAML config** tại `build/android-quality/detekt-android-security.yml` enable cả 10 rules
3. **Merge config** qua `DetektExtension.config.from()` với `buildUponDefaultConfig = true` — không thay thế rule Detekt chuẩn

```kotlin
// Kích hoạt tự động: chỉ cần apply cả hai plugin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.company.android-quality") version "1.0.0"
}

// Hoặc opt-in tường minh:
security { useDetekt = true }
```

**Luồng hoạt động:**
```
AndroidQualityPlugin.apply()
    │
    └─ plugins.withId("io.gitlab.arturbosch.detekt") {
           DetektIntegration.configure(project)
               ├─ addSecurityRulesToDetektClasspath() → detektPlugins += pluginJar
               └─ enableSecurityRulesInConfig()       → detekt.config.from(generatedYaml)
       }
```

### Tiếp theo (v1.3.0+)

| Ưu tiên | Tính năng |
|---------|----------|
| P1 | Tích hợp Slack/Jira — tự động tạo ticket khi phát hiện violation |
