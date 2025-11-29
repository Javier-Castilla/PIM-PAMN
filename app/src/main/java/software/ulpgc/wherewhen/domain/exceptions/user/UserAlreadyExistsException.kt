package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.Email

class UserAlreadyExistsException(email: Email) : UserException("User with email $email already exists");