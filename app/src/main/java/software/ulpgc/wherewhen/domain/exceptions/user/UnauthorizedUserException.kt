package software.ulpgc.wherewhen.domain.exceptions.user

class UnauthorizedUserException(action: String) : UserException("User not authorized to perform: $action");