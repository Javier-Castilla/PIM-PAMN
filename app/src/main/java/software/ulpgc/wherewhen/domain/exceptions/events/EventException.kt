package software.ulpgc.wherewhen.domain.exceptions.events

import software.ulpgc.wherewhen.domain.exceptions.DomainException

abstract class EventException(message: String) : DomainException(message)
