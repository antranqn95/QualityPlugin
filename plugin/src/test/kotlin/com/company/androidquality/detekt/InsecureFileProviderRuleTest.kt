package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.InsecureFileProviderRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InsecureFileProviderRuleTest {
    private val rule = InsecureFileProviderRule(Config.empty)

    @Test
    fun `detects Uri fromFile`() {
        val findings = rule.lint("""
            fun share(file: File) {
                val uri = Uri.fromFile(file)
                intent.data = uri
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureFileProvider")
    }

    @Test
    fun `detects Environment getExternalStorageDirectory`() {
        val findings = rule.lint("""
            fun getStorage(): File {
                return Environment.getExternalStorageDirectory()
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureFileProvider")
    }

    @Test
    fun `detects Environment getExternalStoragePublicDirectory`() {
        val findings = rule.lint("""
            fun getDownloads(): File {
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureFileProvider")
    }

    @Test
    fun `no finding for FileProvider getUriForFile`() {
        val findings = rule.lint("""
            fun share(context: Context, file: File) {
                val uri = FileProvider.getUriForFile(context, "com.example.provider", file)
                intent.data = uri
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for context getExternalFilesDir`() {
        val findings = rule.lint("""
            fun getAppStorage(context: Context): File? {
                return context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for regular Uri operations`() {
        val findings = rule.lint("""
            fun parse(url: String): Uri {
                return Uri.parse(url)
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
