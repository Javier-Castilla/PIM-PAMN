package software.ulpgc.wherewhen.infrastructure.api

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.ulpgc.wherewhen.BuildConfig
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.ports.api.ExternalEventApiService
import software.ulpgc.wherewhen.infrastructure.api.mappers.TicketmasterMapper

class TicketmasterExternalEventApiService : ExternalEventApiService {
    private val apiService: TicketmasterApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://app.ticketmaster.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(TicketmasterApiService::class.java)
    }

    override suspend fun searchNearbyEvents(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val apiKey = BuildConfig.TICKETMASTER_API_KEY
            Log.d("TicketmasterAPI", "API Key length: ${apiKey.length}")
            Log.d("TicketmasterAPI", "API Key first 10 chars: ${apiKey.take(10)}")

            val latLong = "$latitude,$longitude"
            Log.d("TicketmasterAPI", "Searching at: $latLong, radius: $radiusKm")

            val response = apiService.searchEvents(
                apiKey = apiKey,
                latLong = latLong,
                radius = radiusKm
            )

            val events = response.embedded?.events
                ?.map { TicketmasterMapper.toDomain(it) }
                ?: emptyList()

            Log.d("TicketmasterAPI", "Events found: ${events.size}")
            Result.success(events)
        } catch (e: Exception) {
            Log.e("TicketmasterAPI", "Error searching events", e)
            Result.failure(e)
        }
    }

    override suspend fun searchEventsByCategory(
        latitude: Double,
        longitude: Double,
        category: EventCategory,
        radiusKm: Int
    ): Result<List<Event>> {
        return try {
            val apiKey = BuildConfig.TICKETMASTER_API_KEY
            val latLong = "$latitude,$longitude"
            val categoryName = category.name.lowercase()

            Log.d("TicketmasterAPI", "Searching category: $categoryName at $latLong")

            val response = apiService.searchEventsByCategory(
                apiKey = apiKey,
                latLong = latLong,
                radius = radiusKm,
                category = categoryName
            )

            val events = response.embedded?.events
                ?.map { TicketmasterMapper.toDomain(it) }
                ?: emptyList()

            Log.d("TicketmasterAPI", "Events found: ${events.size}")
            Result.success(events)
        } catch (e: Exception) {
            Log.e("TicketmasterAPI", "Error searching by category", e)
            Result.failure(e)
        }
    }
}
