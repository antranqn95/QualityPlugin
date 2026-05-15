package com.example.sampleapp

class PasswordValidator(
    private val minLength: Int = 8,
    private val requireUppercase: Boolean = true,
    private val requireDigit: Boolean = true,
    private val requireSpecialChar: Boolean = true
) {

    fun validate(password: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (password.length < minLength) {
            errors += "Password must be at least $minLength characters long"
        }
        if (requireUppercase && password.none { it.isUpperCase() }) {
            errors += "Password must contain at least one uppercase letter"
        }
        if (requireDigit && password.none { it.isDigit() }) {
            errors += "Password must contain at least one digit"
        }
        if (requireSpecialChar && password.none { it in SPECIAL_CHARS }) {
            errors += "Password must contain at least one special character (!@#\$%^&*)"
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    fun isCommonPassword(password: String): Boolean {
        return password.lowercase() in COMMON_PASSWORDS
    }

    fun calculateStrength(password: String): PasswordStrength {
        val result = validate(password)
        if (!result.isValid) return PasswordStrength.WEAK
        if (isCommonPassword(password)) return PasswordStrength.WEAK
        return when {
            password.length >= 16 && result.errors.isEmpty() -> PasswordStrength.STRONG
            password.length >= 12 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    enum class PasswordStrength { WEAK, MEDIUM, STRONG }

    companion object {
        private const val SPECIAL_CHARS = "!@#\$%^&*()-_=+[]{}|;:,.<>?"
        private val COMMON_PASSWORDS = setOf("password", "12345678", "qwerty123", "admin123")
    }
}
