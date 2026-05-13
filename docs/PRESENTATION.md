# Android Quality Plugin
## Tự động hóa bảo mật & sinh test bằng AI

---

## Slide 1 — Vấn đề

### Dev team đang đối mặt với gì?

**🔴 Bảo mật:**
- Hardcode password, API key vào source code
- Dùng `http://` thay vì `https://`
- Thuật toán mã hóa yếu: MD5, SHA-1, DES
- Log token/password ra Logcat
- Lưu dữ liệu nhạy cảm vào SharedPreferences không mã hóa

**🔴 Testing:**
- Viết test tốn thời gian, dễ bị bỏ qua khi deadline gấp
- Code coverage thấp — đặc biệt ở tầng ViewModel, UseCase, Repository
- Test quality không đồng đều giữa các thành viên

**→ Hậu quả:** Bug lọt vào production, security audit fail, technical debt tăng

---

## Slide 2 — Giải pháp

### Android Quality Plugin

> Một Gradle plugin tích hợp thẳng vào build pipeline — không cần công cụ ngoài, không cần cấu hình phức tạp.

**2 tính năng cốt lõi:**

```
┌─────────────────────────────────────┐
│  ./gradlew checkSecurity            │  ← Phát hiện lỗ hổng bảo mật
│  ./gradlew generateTests            │  ← AI sinh JUnit test tự động
└─────────────────────────────────────┘
```

**Tích hợp chỉ với 10 dòng config:**
```kotlin
androidQuality {
    security { failOnViolation = true }
    ai { apiKey = "sk-ant-..." }
    testGeneration {
        targetPackages = listOf("com.example.app.viewmodel")
    }
}
```

---

## Slide 3 — Tính năng 1: Security Scanner

### 10 quy tắc bảo mật tự động — chuẩn OWASP MASVS

| # | Rule | Phát hiện |
|---|------|-----------|
| 1 | **HardcodedCredentials** | `val password = "abc123"` |
| 2 | **InsecureHttpUsage** | `"http://api.example.com"` |
| 3 | **WeakCryptography** | `MessageDigest.getInstance("MD5")` |
| 4 | **LogSensitiveData** | `Log.d(TAG, "token=$token")` |
| 5 | **UnencryptedSensitiveStorage** | `prefs.putString("password", value)` |
| 6 | **WebViewJavaScriptEnabled** | `webView.settings.javaScriptEnabled = true` |
| 7 | **InsecureRandom** | `val r = Random()` |
| 8 | **ExportedComponent** | `BroadcastReceiver` không có `checkCallingPermission` |
| 9 | **DeepLinkInjection** | `webView.loadUrl(uri.getQueryParameter("url"))` |
| 10 | **InsecureFileProvider** | `Uri.fromFile(file)` — nên dùng `FileProvider` |

**2 chế độ hoạt động:**
- `failOnViolation = false` → Cảnh báo, build vẫn pass *(dùng khi onboarding)*
- `failOnViolation = true` → **Block build** khi có vi phạm *(dùng trong CI/CD)*

**HTML Report (mới):** `generateReport = true` → xuất `security-report.html` với summary và bảng vi phạm có severity badge

---

## Slide 4 — Demo: Security Scanner

### Input — Code có lỗ hổng:
```kotlin
class DataService {
    val password = "admin123"                           // ❌ Hardcoded
    val url = "http://api.company.com/v1"               // ❌ HTTP
    val cipher = Cipher.getInstance("AES/ECB/PKCS5")   // ❌ ECB mode
    
    fun login(token: String) {
        Log.d("Auth", "token=$token")                   // ❌ Log sensitive
    }
}
```

### Output — `./gradlew checkSecurity`:
```
[AndroidQuality Security] HardcodedCredentials: Hardcoded credential detected
  — src/main/java/DataService.kt:2
[AndroidQuality Security] InsecureHttpUsage: Insecure HTTP URL detected
  — src/main/java/DataService.kt:3
[AndroidQuality Security] WeakCryptography: Weak cryptographic algorithm
  — src/main/java/DataService.kt:4
[AndroidQuality Security] LogSensitiveData: Sensitive data in log
  — src/main/java/DataService.kt:7

BUILD FAILED — 4 security violation(s) found
```

---

## Slide 5 — Tính năng 2: AI Test Generation

### Từ source code → JUnit tests hoàn chỉnh

**Input** — `PlaylistViewModel.kt` (47 dòng code)

**Command:** `./gradlew generateTests`

**Output** — `PlaylistViewModelTest.kt` được sinh tự động:

```kotlin
class PlaylistViewModelTest {
    private lateinit var fetchPlaylistUseCase: FetchPlaylistUseCase
    private lateinit var viewModel: PlaylistViewModel

    @Before fun setUp() {
        fetchPlaylistUseCase = mockk()
        viewModel = PlaylistViewModel(fetchPlaylistUseCase, mockk())
    }

    @Test
    fun `loadCategory sets uiState to Success when use case returns channels`() {
        val channels = listOf(Channel("1", "Sports HD"))
        every { fetchPlaylistUseCase.execute("sports") } returns
            FetchPlaylistUseCase.Result.Success(channels)

        viewModel.loadCategory("sports")

        val state = viewModel.uiState as PlaylistViewModel.UiState.Success
        assertEquals(channels, state.channels)
    }

    @Test
    fun `loadCategory sets uiState to Error when category is blank`() { ... }

    @Test
    fun `search returns empty list when query is too short`() { ... }
    // ... và nhiều test cases khác
}
```

**⏱ Thời gian:** ~30 giây/class thay vì 2-4 giờ viết tay

**AI Cache (mới):** `useCache = true` → lưu SHA-256 hash của source — không gọi API lại nếu code chưa thay đổi

---

## Slide 6 — Kiến trúc kỹ thuật

```
┌──────────────────────────────────────────────────────┐
│              Android Quality Plugin                  │
│                                                      │
│  ┌──────────────────┐  ┌───────────────────────────┐ │
│  │  CheckSecurity   │  │     GenerateTests         │ │
│  │     Task         │  │         Task              │ │
│  └────────┬─────────┘  └──────────┬────────────────┘ │
│           │                       │                  │
│           ▼                       ▼                  │
│  ┌────────────────┐   ┌─────────────────────────┐   │
│  │ 7 Detekt Rules │   │  KotlinCodeAnalyzer      │   │
│  │ (Visitor/AST)  │   │  (Regex parser)          │   │
│  └────────────────┘   └──────────┬──────────────┘   │
│                                  │                  │
│                                  ▼                  │
│                       ┌─────────────────────────┐   │
│                       │   AITestGenerator        │   │
│                       │   Java 11 HttpClient     │   │
│                       │   → Anthropic Claude API │   │
│                       └─────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

**Extension DSL** cho phép cấu hình linh hoạt trong `build.gradle.kts`

---

## Slide 7 — Công nghệ & Lý do lựa chọn

### Tại sao Detekt?

| | Detekt ✅ | Android Lint | SonarQube |
|-|-----------|-------------|-----------|
| Kotlin-native | ✅ | ⚠️ | ⚠️ |
| Custom rule bằng Kotlin | ✅ Dễ | ❌ Phức tạp | ❌ |
| Chạy offline, không cần server | ✅ | ✅ | ❌ |
| Tích hợp Gradle native | ✅ | ✅ | ⚠️ |

### Tại sao Claude AI?

- **Context window 200K tokens** — đọc được cả file source phức tạp
- **Code quality cao** — sinh test compile được, không cần sửa nhiều
- **Prompt-following tốt** — trả đúng format Kotlin, không kèm markdown
- **API cost hợp lý** — `claude-sonnet-4-6` cân bằng tốt giữa chất lượng và giá

### Tại sao Gradle Plugin?

- **Zero friction** — dev chỉ cần `./gradlew checkSecurity`
- **CI/CD native** — tự động chạy trong pipeline, không cần cấu hình thêm
- **Cấu hình trong project** — versioned cùng source code

---

## Slide 8 — Kết quả đo lường

### Với sample-app demo:

```
📊 Security Scan
   10 loại lỗ hổng được phát hiện tự động
   Thời gian scan: < 2 giây cho 10 files
   HTML report: security-report.html với summary + bảng findings

🤖 AI Test Generation
   3 class mới (ViewModel, UseCase, Repository)
   → 3 test files sinh trong ~90 giây
   → 299 tests PASS ngay không cần sửa (sau fix nhỏ về sealed class)
   AI cache: bỏ qua API call khi source chưa đổi

✅ Plugin Test Coverage
   121 unit + integration tests
   0 failures
```

### So sánh thời gian:

| | Thủ công | Với plugin |
|-|---------|-----------|
| Scan security (10 files) | ~2 giờ code review | **< 2 giây** |
| Viết test cho 1 ViewModel | 2–4 giờ | **~30 giây** |
| Onboard rule mới | Training team | **1 commit** |

---

## Slide 9 — Cách tích hợp vào dự án

### 3 bước, 5 phút

**Bước 1** — Thêm repository:
```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven { url = uri("https://maven.pkg.github.com/antranqn95/QualityPlugin") }
    }
}
```

**Bước 2** — Apply plugin:
```kotlin
// build.gradle.kts
plugins {
    id("com.company.android-quality") version "1.0.0"
}
```

**Bước 3** — Cấu hình và chạy:
```bash
./gradlew checkSecurity    # Xong ngay
./gradlew generateTests    # Sinh test cho toàn bộ package
```

**Thêm vào CI** (GitHub Actions / Bitrise / Jenkins):
```yaml
- run: ./gradlew checkSecurity
```

---

## Slide 10 — Roadmap

### Đã hoàn thành (v1.1.0)
- ✅ Rule mới: `ExportedComponent`, `DeepLinkInjection`, `InsecureFileProvider` (tổng 10 rules)
- ✅ Hỗ trợ `src/main/kotlin` lẫn `src/main/java`
- ✅ HTML report cho security scan
- ✅ Cache AI response theo SHA-256 hash — không gọi API lại khi code chưa đổi

### Đã hoàn thành (v1.2.0)
- ✅ **IDE Plugin** — 10 inspections highlight real-time trong Android Studio, cài từ file `.zip`
- ✅ **StateFlow/SharedFlow support** — AI tự động sinh test dùng Turbine + `runTest`
- ✅ **Detekt integration** — auto-wire 10 MASVS rules vào `./gradlew detekt` khi cả hai plugin cùng applied

### Tiếp theo (v1.3.0+)
- ✦ Tích hợp với Slack/Jira — tự động tạo ticket khi phát hiện violation

---

## Slide 11 — Tổng kết

### Android Quality Plugin giải quyết được

| Vấn đề | Giải pháp |
|--------|----------|
| Security slip vào production | `checkSecurity` block build tự động |
| Code coverage thấp | `generateTests` sinh test trong 30 giây |
| Inconsistent code quality | Quy tắc chuẩn hóa cho cả team |
| Onboarding rule mới chậm | Chỉ cần add rule vào plugin, toàn team được ngay |

### Một dòng lệnh, hai tác dụng:
```bash
./gradlew checkSecurity generateTests
```
> **Bảo mật tốt hơn. Test nhiều hơn. Ít công sức hơn.**
