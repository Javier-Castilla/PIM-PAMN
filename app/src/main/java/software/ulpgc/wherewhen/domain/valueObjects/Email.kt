package software.ulpgc.wherewhen.domain.valueObjects

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        private val EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        fun create(email: String): Result<Email> {
            return if (email.isNotBlank() && EMAIL_REGEX.matches(email)) {
                Result.success(Email(email.lowercase()))
            } else {
                Result.failure(IllegalArgumentException("Invalid email"))
            }
        }
    }
}

