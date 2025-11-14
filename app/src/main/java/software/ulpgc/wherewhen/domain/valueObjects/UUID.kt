package software.ulpgc.wherewhen.domain.valueObjects

@JvmInline
value class UUID private constructor(val value: String) {
    companion object {
        private val UUID_REGEX =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
                .toRegex(RegexOption.IGNORE_CASE)

        fun parse(id: String): Result<UUID> {
            return if (UUID_REGEX.matches(id)) {
                Result.success(UUID(id.lowercase()))
            } else {
                Result.failure(IllegalArgumentException("Invalid UUID: $id"))
            }
        }

        fun random(): UUID {
            val uuid = java.util.UUID.randomUUID().toString()
            return UUID(uuid)
        }
    }

    override fun toString(): String = value
}