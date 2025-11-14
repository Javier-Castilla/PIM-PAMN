package software.ulpgc.wherewhen.domain.exceptions.user

class InvalidPasswordException(reason: String) : UserException("Invalid password: $reason");