package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.ExportedComponentRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExportedComponentRuleTest {
    private val rule = ExportedComponentRule(Config.empty)

    @Test
    fun `detects BroadcastReceiver without permission check`() {
        val findings = rule.lint("""
            class MyReceiver : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    doSomething()
                }
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("ExportedComponent")
    }

    @Test
    fun `detects Service without permission check`() {
        val findings = rule.lint("""
            class MyService : Service() {
                override fun onBind(intent: Intent) = null
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("ExportedComponent")
    }

    @Test
    fun `detects ContentProvider without permission check`() {
        val findings = rule.lint("""
            class MyProvider : ContentProvider() {
                override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?) = null
                override fun insert(uri: Uri, values: ContentValues?) = null
                override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
                override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
                override fun getType(uri: Uri) = null
                override fun onCreate() = false
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("ExportedComponent")
    }

    @Test
    fun `no finding for BroadcastReceiver with checkCallingPermission`() {
        val findings = rule.lint("""
            class MyReceiver : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (context.checkCallingPermission("com.example.PERMISSION") != PackageManager.PERMISSION_GRANTED) return
                    doSomething()
                }
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for Service with enforceCallingPermission`() {
        val findings = rule.lint("""
            class MyService : Service() {
                override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
                    enforceCallingPermission("com.example.PERMISSION", "Need permission")
                    return START_STICKY
                }
                override fun onBind(intent: Intent) = null
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for regular class not extending Android component`() {
        val findings = rule.lint("""
            class MyViewModel : ViewModel() {
                fun doSomething() {}
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
