package software.ulpgc.wherewhen.domain.model.events

import kotlin.math.*

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val placeName: String?,
    val city: String?,
    val country: String?
) {
    fun distanceTo(other: Location): Double {
        val R = 6371.0
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)
        
        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    fun formatAddress(): String {
        return buildString {
            placeName?.let { append("$it, ") }
            address?.let { append("$it, ") }
            city?.let { append(it) }
        }.trimEnd(',', ' ')
    }
}
