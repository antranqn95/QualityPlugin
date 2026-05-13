package com.example.sampleapp

// Demo: UnencryptedSensitiveStorage anti-pattern
class PrefsHelper {
    fun saveToken(token: String) = println("putString auth_token $token")
    fun getToken(): String? = run { println("getString auth_token"); null }
    fun savePassword(pw: String) = println("putString password_key $pw")
}
