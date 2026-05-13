package com.example.sampleapp

class UserAuthManager {

    private var currentUser: String? = null
    private var sessionToken: String? = null
    private val loginAttempts = mutableMapOf<String, Int>()

    fun login(username: String, password: String): Result<String> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password must not be blank"))
        }
        val attempts = loginAttempts.getOrDefault(username, 0)
        if (attempts >= 5) {
            return Result.failure(SecurityException("Account locked after too many failed attempts"))
        }
        if (password.length < 6) {
            loginAttempts[username] = attempts + 1
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }
        currentUser = username
        sessionToken = "token_${username}_${System.currentTimeMillis()}"
        loginAttempts.remove(username)
        return Result.success(sessionToken!!)
    }

    fun logout() {
        currentUser = null
        sessionToken = null
    }

    fun isLoggedIn(): Boolean = currentUser != null && sessionToken != null

    fun getCurrentUser(): String? = currentUser

    fun resetFailedAttempts(username: String) {
        loginAttempts.remove(username)
    }

    fun getFailedAttempts(username: String): Int = loginAttempts.getOrDefault(username, 0)
}
