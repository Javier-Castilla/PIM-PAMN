package software.ulpgc.wherewhen.infrastructure.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.ulpgc.wherewhen.BuildConfig
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.ports.api.ExternalEventApiService
import software.ulpgc.wherewhen.infrastructure.api.TicketmasterApiService
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
            val latLong = "$latitude,$longitude"
            val response = apiService.searchEvents(
                apiKey = BuildConfig.TICKETMASTER_API_KEY,
                latLong = latLong,
                radius = radiusKm
            )
            
            val events = response.embedded?.events
                ?.map { TicketmasterMapper.toDomain(it) }
                ?: emptyList()
            
            Result.success(events)
        } catch (e: Exception) {
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
            val latLong = "$latitude,$longitude"
            val categoryName = category.name.lowercase()
            
            val response = apiService.searchEventsByCategory(
                apiKey = BuildConfig.TICKETMASTER_API_KEY,
                latLong = latLong,
                radius = radiusKm,
                category = categoryName
            )
            
            val events = response.embedded?.events
                ?.map { TicketmasterMapper.toDomain(it) }
                ?: emptyList()
            
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
