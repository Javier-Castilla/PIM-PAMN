package software.ulpgc.wherewhen.domain.exceptions.events

class InvalidEventException(message: String) : 
    EventException("Invalid event: $message")
