package software.ulpgc.wherewhen.domain.ports.location

import software.ulpgc.wherewhen.domain.model.events.Location

interface LocationService {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun geocodeAddress(address: String): Result<Location>
    fun hasLocationPermission(): Boolean
}
