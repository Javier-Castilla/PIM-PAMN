package software.ulpgc.wherewhen.domain.exceptions.user

class UserNotFoundException(identifier: Any) : UserException("User not found: $identifier")
