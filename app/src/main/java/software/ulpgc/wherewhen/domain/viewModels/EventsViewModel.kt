package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.valueObjects.UUID

interface EventsViewModel {
    fun onTabSelected(index: Int)
    fun onSearchQueryChange(query: String)
    fun onCategorySelected(category: EventCategory?)
    fun onRefresh()
    fun loadNearbyEvents()
    fun loadMyEvents()
    fun clearSearch()
}

interface EventDetailViewModel {
    fun loadEvent(eventId: UUID)
    fun onJoinEvent()
    fun onLeaveEvent()
    fun loadAttendees()
}
