package software.ulpgc.wherewhen.domain.exceptions.events

class UnauthorizedEventAccessException : 
    EventException("You are not authorized to modify this event")
