package software.ulpgc.wherewhen.domain.exceptions.user

import software.ulpgc.wherewhen.domain.exceptions.DomainException

sealed class UserException(message: String) : DomainException(message) {
}