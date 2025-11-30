package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.Email

class InvalidEmailException(email: Email) : UserException("Invalid email format: $email");