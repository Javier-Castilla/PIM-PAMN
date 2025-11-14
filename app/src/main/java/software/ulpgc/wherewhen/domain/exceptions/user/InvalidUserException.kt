package software.ulpgc.wherewhen.domain.exceptions.user

class InvalidUserException(reason: String): UserException("User not found: $reason");