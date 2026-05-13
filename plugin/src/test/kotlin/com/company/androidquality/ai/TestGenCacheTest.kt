package com.company.androidquality.ai

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class TestGenCacheTest {

    @TempDir
    lateinit var tempDir: Path

    private fun cacheFile(): File = tempDir.resolve("cache.json").toFile()

    @Test
    fun `isUpToDate returns false for unknown class`() {
        val cache = TestGenCache(cacheFile())
        assertThat(cache.isUpToDate("MyClass", "class MyClass {}")).isFalse()
    }

    @Test
    fun `isUpToDate returns true after put with same source`() {
        val cache = TestGenCache(cacheFile())
        cache.put("MyClass", "class MyClass {}")
        assertThat(cache.isUpToDate("MyClass", "class MyClass {}")).isTrue()
    }

    @Test
    fun `isUpToDate returns false when source changes`() {
        val cache = TestGenCache(cacheFile())
        cache.put("MyClass", "class MyClass {}")
        assertThat(cache.isUpToDate("MyClass", "class MyClass { fun foo() {} }")).isFalse()
    }

    @Test
    fun `cache persists to disk and survives re-instantiation`() {
        val file = cacheFile()
        val cache1 = TestGenCache(file)
        cache1.put("MyClass", "class MyClass {}")

        val cache2 = TestGenCache(file)
        assertThat(cache2.isUpToDate("MyClass", "class MyClass {}")).isTrue()
    }

    @Test
    fun `different classes tracked independently`() {
        val cache = TestGenCache(cacheFile())
        cache.put("ClassA", "class ClassA {}")
        cache.put("ClassB", "class ClassB {}")

        assertThat(cache.isUpToDate("ClassA", "class ClassA {}")).isTrue()
        assertThat(cache.isUpToDate("ClassB", "class ClassB {}")).isTrue()
        assertThat(cache.isUpToDate("ClassA", "class ClassA { fun changed() {} }")).isFalse()
    }

    @Test
    fun `isUpToDate returns false when cache file does not exist`() {
        val cache = TestGenCache(File(tempDir.toFile(), "nonexistent/cache.json"))
        assertThat(cache.isUpToDate("Any", "source")).isFalse()
    }

    @Test
    fun `put creates cache file and parent directories`() {
        val nested = File(tempDir.toFile(), "a/b/c/cache.json")
        val cache = TestGenCache(nested)
        cache.put("MyClass", "class MyClass {}")
        assertThat(nested).exists()
    }
}
