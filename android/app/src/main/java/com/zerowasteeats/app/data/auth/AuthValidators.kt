package com.zerowasteeats.app.data.auth

/** Why a form field is not valid. The UI maps each value to a string. */
enum class ValidationError {
    REQUIRED,
    NAME_TOO_SHORT,
    NAME_TOO_LONG,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT
}

/**
 * Rules for the sign in and sign up forms. They match the server's DTOs (name of 2 to 80
 * characters) so a value accepted here is not rejected there. Each function returns null
 * when the value is valid.
 */
object AuthValidators {
    const val NAME_MIN_LENGTH = 2
    const val NAME_MAX_LENGTH = 80
    const val PASSWORD_MIN_LENGTH = 8
    private const val EMAIL_MAX_LENGTH = 254

    private val emailPattern =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** Surrounding spaces are ignored, as the server trims the name too. */
    fun validateName(value: String): ValidationError? {
        val name = value.trim()
        return when {
            name.isEmpty() -> ValidationError.REQUIRED
            name.length < NAME_MIN_LENGTH -> ValidationError.NAME_TOO_SHORT
            name.length > NAME_MAX_LENGTH -> ValidationError.NAME_TOO_LONG
            else -> null
        }
    }

    fun validateEmail(value: String): ValidationError? {
        val email = value.trim()
        return when {
            email.isEmpty() -> ValidationError.REQUIRED
            email.length > EMAIL_MAX_LENGTH || !emailPattern.matches(email) ->
                ValidationError.EMAIL_INVALID
            else -> null
        }
    }

    /** For sign up: spaces count as characters, so the password is not trimmed. */
    fun validateNewPassword(value: String): ValidationError? = when {
        value.isEmpty() -> ValidationError.REQUIRED
        value.length < PASSWORD_MIN_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        else -> null
    }

    private val repeatedSpaces = Regex("\\s+")

    /** Trims the ends and collapses every run of whitespace into one space. */
    fun collapseSpaces(value: String): String = value.trim().replace(repeatedSpaces, " ")

    /**
     * For people's names and cities: [collapseSpaces], then the first letter of each word in
     * capitals. The rest of each word is left as typed, so "McDonald" or "NY" survive.
     */
    fun capitalizeWords(value: String): String =
        collapseSpaces(value).split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }

    /** Business name, city, or the password on sign in: it only has to be filled in. */
    fun validateRequired(value: String): ValidationError? =
        if (value.isBlank()) ValidationError.REQUIRED else null
}
