package software.ulpgc.wherewhen.domain.valueObjects

@JvmInline
value class UUID private constructor(val value: String) {
    companion object {
        private val STANDARD_UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
                .toRegex()

        private val FIREBASE_UID_REGEX = "^[a-zA-Z0-9]{28}$".toRegex()

        fun parse(id: String): Result<UUID> {
            return when {
                id.isBlank() ->
                    Result.failure(IllegalArgumentException("UUID cannot be blank"))
                STANDARD_UUID_REGEX.matches(id) ->
                    Result.success(UUID(id.lowercase()))
                FIREBASE_UID_REGEX.matches(id) ->
                    Result.success(UUID(id))
                else ->
                    Result.failure(IllegalArgumentException("Invalid UUID format: $id"))
            }
        }

        fun random(): UUID {
            val uuid = java.util.UUID.randomUUID().toString()
            return UUID(uuid)
        }
    }

    override fun toString(): String = value
}
