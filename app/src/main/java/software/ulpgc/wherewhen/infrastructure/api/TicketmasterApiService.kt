package software.ulpgc.wherewhen.infrastructure.api

import retrofit2.http.GET
import retrofit2.http.Query
import software.ulpgc.wherewhen.infrastructure.api.models.TicketmasterResponse

interface TicketmasterApiService {
    @GET("discovery/v2/events.json")
    suspend fun searchEvents(
        @Query("apikey") apiKey: String,
        @Query("latlong") latLong: String,
        @Query("radius") radius: Int,
        @Query("unit") unit: String = "km",
        @Query("size") size: Int = 50,
        @Query("sort") sort: String = "date,asc"
    ): TicketmasterResponse
    
    @GET("discovery/v2/events.json")
    suspend fun searchEventsByCategory(
        @Query("apikey") apiKey: String,
        @Query("latlong") latLong: String,
        @Query("radius") radius: Int,
        @Query("classificationName") category: String,
        @Query("unit") unit: String = "km",
        @Query("size") size: Int = 50,
        @Query("sort") sort: String = "date,asc"
    ): TicketmasterResponse
}
