package com.company.androidquality.ai

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinCodeAnalyzerTest {

    @TempDir
    lateinit var tempDir: File

    private val analyzer = KotlinCodeAnalyzer()

    @Test
    fun `analyze returns empty list when directory does not exist`() {
        val nonExistent = File(tempDir, "nonexistent")
        val result = analyzer.analyze(nonExistent)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze returns empty list when directory is empty`() {
        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze returns empty list for non-kt files`() {
        tempDir.resolve("SomeFile.java").writeText("public class SomeFile {}")
        tempDir.resolve("notes.txt").writeText("some notes")

        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze returns empty list for kt file without package declaration`() {
        tempDir.resolve("NoPackage.kt").writeText("""
            class NoPackage {
                fun doSomething() {}
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze returns empty list for kt file without class declaration`() {
        tempDir.resolve("OnlyFunctions.kt").writeText("""
            package com.example

            fun topLevelFunction() {}
            val topLevelVal = "hello"
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze returns ClassInfo for valid kt file with simple class`() {
        tempDir.resolve("MyClass.kt").writeText("""
            package com.example.app

            class MyClass {
                fun doWork() {}
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("MyClass")
        assertThat(result[0].packageName).isEqualTo("com.example.app")
        assertThat(result[0].packagePath).isEqualTo("com/example/app")
    }

    @Test
    fun `analyze returns ClassInfo for data class`() {
        tempDir.resolve("DataModel.kt").writeText("""
            package com.example.model

            data class DataModel(val id: String, val name: String)
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("DataModel")
        assertThat(result[0].packageName).isEqualTo("com.example.model")
    }

    @Test
    fun `analyze returns ClassInfo for open class`() {
        tempDir.resolve("BaseClass.kt").writeText("""
            package com.example

            open class BaseClass {
                open fun method() {}
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("BaseClass")
    }

    @Test
    fun `analyze returns ClassInfo for abstract class`() {
        tempDir.resolve("AbstractBase.kt").writeText("""
            package com.example

            abstract class AbstractBase {
                abstract fun method()
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("AbstractBase")
    }

    @Test
    fun `analyze returns ClassInfo for sealed class`() {
        tempDir.resolve("SealedState.kt").writeText("""
            package com.example

            sealed class SealedState {
                object Loading : SealedState()
                data class Success(val data: String) : SealedState()
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("SealedState")
    }

    @Test
    fun `analyze skips interface declarations`() {
        tempDir.resolve("MyInterface.kt").writeText("""
            package com.example

            interface MyInterface {
                fun doSomething()
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze skips object declarations`() {
        tempDir.resolve("Singleton.kt").writeText("""
            package com.example

            object Singleton {
                fun getInstance() = this
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).isEmpty()
    }

    @Test
    fun `analyze picks first class when file has multiple classes`() {
        tempDir.resolve("MultiClass.kt").writeText("""
            package com.example

            class FirstClass {
                fun method() {}
            }

            class SecondClass {
                fun other() {}
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("FirstClass")
    }

    @Test
    fun `analyze returns multiple ClassInfos for multiple kt files`() {
        tempDir.resolve("Alpha.kt").writeText("""
            package com.example

            class Alpha {}
        """.trimIndent())
        tempDir.resolve("Beta.kt").writeText("""
            package com.example

            class Beta {}
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(2)
        assertThat(result.map { it.className }).containsExactlyInAnyOrder("Alpha", "Beta")
    }

    @Test
    fun `analyze includes full source content in ClassInfo`() {
        val source = """
            package com.example

            class WithSource {
                fun method(): String = "hello"
            }
        """.trimIndent()
        tempDir.resolve("WithSource.kt").writeText(source)

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].source).isEqualTo(source)
    }

    @Test
    fun `analyze walks subdirectories recursively`() {
        val subDir = File(tempDir, "sub/dir").also { it.mkdirs() }
        subDir.resolve("Nested.kt").writeText("""
            package com.example.sub

            class Nested {}
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result).hasSize(1)
        assertThat(result[0].className).isEqualTo("Nested")
        assertThat(result[0].packageName).isEqualTo("com.example.sub")
    }

    @Test
    fun `analyze packagePath replaces dots with slashes`() {
        tempDir.resolve("Deep.kt").writeText("""
            package com.example.deep.pkg

            class Deep {}
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].packagePath).isEqualTo("com/example/deep/pkg")
    }

    // region hasFlowTypes

    @Test
    fun `analyze sets hasFlowTypes false for class without flow properties`() {
        tempDir.resolve("Plain.kt").writeText("""
            package com.example

            class Plain {
                fun doWork() {}
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].hasFlowTypes).isFalse()
    }

    @Test
    fun `analyze sets hasFlowTypes true for class with StateFlow`() {
        tempDir.resolve("StateViewModel.kt").writeText("""
            package com.example

            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.MutableStateFlow

            class StateViewModel {
                private val _state = MutableStateFlow<String>("initial")
                val state: StateFlow<String> = _state
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].hasFlowTypes).isTrue()
    }

    @Test
    fun `analyze sets hasFlowTypes true for class with SharedFlow`() {
        tempDir.resolve("EventViewModel.kt").writeText("""
            package com.example

            import kotlinx.coroutines.flow.MutableSharedFlow

            class EventViewModel {
                private val _events = MutableSharedFlow<String>()
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].hasFlowTypes).isTrue()
    }

    @Test
    fun `analyze sets hasFlowTypes true for class with Flow return type`() {
        tempDir.resolve("FlowRepo.kt").writeText("""
            package com.example

            import kotlinx.coroutines.flow.Flow

            class FlowRepo {
                fun getItems(): Flow<List<String>> = TODO()
            }
        """.trimIndent())

        val result = analyzer.analyze(tempDir)
        assertThat(result[0].hasFlowTypes).isTrue()
    }

    // endregion
}
