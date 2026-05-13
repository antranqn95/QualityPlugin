package com.example.sampleapp

// Demo: InsecureHttpUsage anti-pattern
object NetworkConfig {
    val baseUrl = "http://api.internal.company.com"
    val fallbackUrl = "http://10.0.0.1:8080/api"
}
