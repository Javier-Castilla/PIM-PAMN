// infrastructure/api/models/TicketmasterModels.kt
package software.ulpgc.wherewhen.infrastructure.api.models

import com.google.gson.annotations.SerializedName

data class TicketmasterResponse(
    @SerializedName("_embedded")
    val embedded: EmbeddedEvents?,
    val page: PageInfo?
)

data class EmbeddedEvents(
    val events: List<TicketmasterEvent>?
)

data class PageInfo(
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val number: Int
)

data class TicketmasterEvent(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
    val images: List<TicketmasterImage>?,
    val dates: TicketmasterDates,
    val classifications: List<TicketmasterClassification>?,
    val priceRanges: List<TicketmasterPriceRange>?,
    val distance: Double?,
    val units: String?,
    @SerializedName("_embedded")
    val embedded: TicketmasterEmbedded?
)

data class TicketmasterImage(
    val url: String,
    val width: Int,
    val height: Int,
    val ratio: String
)

data class TicketmasterDates(
    val start: TicketmasterStartDate,
    val timezone: String?,
    val status: TicketmasterStatus?
)

data class TicketmasterStartDate(
    val localDate: String?,
    val localTime: String?,
    val dateTime: String?
)

data class TicketmasterStatus(
    val code: String
)

data class TicketmasterClassification(
    val primary: Boolean,
    val segment: TicketmasterSegment?,
    val genre: TicketmasterGenre?,
    val subGenre: TicketmasterSubGenre?
)

data class TicketmasterSegment(
    val id: String,
    val name: String
)

data class TicketmasterGenre(
    val id: String,
    val name: String
)

data class TicketmasterSubGenre(
    val id: String,
    val name: String
)

data class TicketmasterPriceRange(
    val min: Double,
    val max: Double,
    val currency: String
)

data class TicketmasterEmbedded(
    val venues: List<TicketmasterVenue>?,
    val attractions: List<TicketmasterAttraction>?
)

data class TicketmasterVenue(
    val name: String,
    val type: String?,
    val id: String,
    val url: String?,
    val postalCode: String?,
    val timezone: String?,
    val city: TicketmasterCity?,
    val state: TicketmasterState?,
    val country: TicketmasterCountry?,
    val address: TicketmasterAddress?,
    val location: TicketmasterLocation?
)

data class TicketmasterCity(
    val name: String
)

data class TicketmasterState(
    val name: String?
)

data class TicketmasterCountry(
    val name: String,
    val countryCode: String
)

data class TicketmasterAddress(
    val line1: String?
)

data class TicketmasterLocation(
    val latitude: String,
    val longitude: String
)

data class TicketmasterAttraction(
    val name: String,
    val id: String,
    val url: String?,
    val images: List<TicketmasterImage>?
)
