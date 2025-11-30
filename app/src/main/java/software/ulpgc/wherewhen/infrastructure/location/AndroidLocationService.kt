package software.ulpgc.wherewhen.infrastructure.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService

class AndroidLocationService(private val context: Context) : LocationService {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> {
        return try {
            if (!hasLocationPermission()) {
                Log.e("LocationService", "No hay permisos de ubicación")
                return Result.failure(SecurityException("Location permission not granted"))
            }

            Log.d("LocationService", "Obteniendo ubicación...")
            val cancellationToken = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()

            if (location != null) {
                Log.d("LocationService", "Ubicación obtenida: ${location.latitude}, ${location.longitude}")
                Result.success(
                    Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = null,
                        placeName = null,
                        city = null,
                        country = null
                    )
                )
            } else {
                Log.e("LocationService", "Location es null")
                Result.failure(Exception("Unable to get location"))
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
