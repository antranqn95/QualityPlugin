package com.company.androidquality.ai

import java.io.File
import java.security.MessageDigest

class TestGenCache(private val cacheFile: File) {

    private val entries: MutableMap<String, String> = mutableMapOf()

    init {
        if (cacheFile.exists()) {
            load()
        }
    }

    fun isUpToDate(className: String, source: String): Boolean {
        val stored = entries[className] ?: return false
        return stored == sha256(source)
    }

    fun put(className: String, source: String) {
        entries[className] = sha256(source)
        save()
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun load() {
        try {
            val parsed = groovy.json.JsonSlurper().parse(cacheFile) as? Map<String, String> ?: return
            entries.putAll(parsed)
        } catch (_: Exception) {
            // corrupt cache — start fresh
        }
    }

    private fun save() {
        cacheFile.parentFile.mkdirs()
        cacheFile.writeText(groovy.json.JsonOutput.toJson(entries))
    }
}
