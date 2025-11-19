package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.valueObjects.Email

class UserWithEmailNotFoundException(email: Email) : UserException("User not found with email $email");
