package software.ulpgc.wherewhen.infrastructure.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import java.util.Locale

class AndroidLocationService(private val context: Context) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Location> {
        return try {
            if (!hasLocationPermission()) {
                Log.e("LocationService", "No hay permisos de ubicación")
                return Result.failure(SecurityException("Location permission not granted"))
            }

            Log.d("LocationService", "Obteniendo ubicación...")

            val location = try {
                withTimeout(5000L) {
                    val cancellationToken = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationToken.token
                    ).await()
                }
            } catch (e: Exception) {
                Log.w("LocationService", "getCurrentLocation falló, usando lastLocation: ${e.message}")
                fusedLocationClient.lastLocation.await()
            }

            if (location != null) {
                Log.d("LocationService", "Ubicación obtenida: ${location.latitude}, ${location.longitude}")

                val addressInfo = try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.let { address ->
                        Triple(
                            address.getAddressLine(0),
                            address.locality,
                            address.countryName
                        )
                    }
                } catch (e: Exception) {
                    Log.w("LocationService", "Geocoding falló: ${e.message}")
                    null
                }

                Result.success(
                    Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = addressInfo?.first,
                        placeName = null,
                        city = addressInfo?.second,
                        country = addressInfo?.third
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
