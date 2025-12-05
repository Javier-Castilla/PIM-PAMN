package software.ulpgc.wherewhen.domain.viewModels

import software.ulpgc.wherewhen.domain.model.events.EventCategory
import java.time.LocalDate
import java.time.LocalTime

interface CreateEventViewModel {
    fun onTitleChange(value: String)
    fun onDescriptionChange(value: String)
    fun onCategoryChange(category: EventCategory)
    fun onDateChange(date: LocalDate)
    fun onTimeChange(time: LocalTime)
    fun onEndDateChange(date: LocalDate?)
    fun onEndTimeChange(time: LocalTime?)
    fun onMaxAttendeesChange(value: String)
    fun onLocationAddressChange(value: String)
    fun onUseCurrentLocationChange(value: Boolean)
    fun createEvent(onSuccess: () -> Unit)
    fun clearError()
}
