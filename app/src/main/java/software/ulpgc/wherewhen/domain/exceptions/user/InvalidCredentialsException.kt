package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.exceptions.DomainException

class InvalidCredentialsException : DomainException("Invalid email or password")