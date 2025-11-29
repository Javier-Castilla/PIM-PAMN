package software.ulpgc.wherewhen.domain.ports.location

import software.ulpgc.wherewhen.domain.model.events.Location

interface LocationService {
    suspend fun getCurrentLocation(): Result<Location>
    fun hasLocationPermission(): Boolean
}
