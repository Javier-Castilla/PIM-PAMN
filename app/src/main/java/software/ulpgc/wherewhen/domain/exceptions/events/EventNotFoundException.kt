package software.ulpgc.wherewhen.domain.exceptions.events

class EventNotFoundException(eventId: String) : 
    EventException("Event with ID $eventId not found")
