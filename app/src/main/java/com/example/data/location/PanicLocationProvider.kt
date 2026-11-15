package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

data class GpsCoordinateFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val addressLabel: String,
    val isLiveGps: Boolean
)

object PanicLocationProvider {

    const val UNSET_LAT = 0.0
    const val UNSET_LNG = 0.0

    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
    }

    fun buildGoogleMapsLink(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", latitude, longitude)
    }

    fun buildPanicDispatchNotes(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        isLiveGps: Boolean,
        emergencyContact: String?
    ): String {
        val coordText = formatCoordinates(latitude, longitude)
        val accuracyText = accuracyMeters?.let { "±${it.toInt()}m" } ?: "Standard"
        val gpsOrigin = if (isLiveGps) "LIVE GPS FIX ($accuracyText)" else "LAST KNOWN / SAVED SITE"
        val contactNote = emergencyContact?.takeIf { it.isNotBlank() }?.let { " | ICE: $it" } ?: ""
        return "HIGH-PRIORITY PANIC ALERT | $gpsOrigin | Coords: $coordText$contactNote"
    }

    fun buildEmergencyContactAlertMessage(
        senderName: String?,
        emergencyContactName: String?,
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        isLiveGps: Boolean,
        accuracyMeters: Float?
    ): String {
        val cleanSender = senderName?.trim()?.takeIf { it.isNotBlank() } ?: "SeQure User"
        val greeting = emergencyContactName?.trim()?.takeIf { it.isNotBlank() }?.let { "$it, " } ?: ""
        val hasValidCoords = latitude != 0.0 || longitude != 0.0
        val coords = formatCoordinates(latitude, longitude)
        val mapsLink = buildGoogleMapsLink(latitude, longitude)
        val accuracyTag = accuracyMeters?.let { " (±${it.toInt()}m)" } ?: ""
        val fixType = if (isLiveGps) "Live GPS$accuracyTag" else "Last Known Site"

        return buildString {
            append("SOS EMERGENCY ALERT from $cleanSender! ")
            if (greeting.isNotEmpty()) {
                append("${greeting}I need immediate assistance.\n")
            } else {
                append("I need immediate assistance.\n")
            }
            append("Location ($fixType): $locationLabel\n")
            if (hasValidCoords) {
                append("GPS: $coords\n")
                append("Map Pin: $mapsLink\n")
            } else {
                append("GPS: Unavailable (device location pending)\n")
            }
            append("Security dispatch has also been alerted.")
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun acquireCurrentGpsLocation(
        context: Context,
        fallbackLocationName: String? = null
    ): GpsCoordinateFix {
        val fallbackLabel = fallbackLocationName?.takeIf { it.isNotBlank() }
            ?: "Registered Site (GPS Unavailable)"

        if (!hasLocationPermission(context)) {
            return GpsCoordinateFix(
                latitude = UNSET_LAT,
                longitude = UNSET_LNG,
                accuracyMeters = null,
                addressLabel = fallbackLabel,
                isLiveGps = false
            )
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        var resolvedLocation: Location? = null

        // 1. Attempt fresh high-accuracy GPS fix
        val cts = CancellationTokenSource()
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(15_000L)
                .setDurationMillis(4_000L)
                .build()

            resolvedLocation = withTimeoutOrNull(4_500L) {
                fusedClient.getCurrentLocation(request, cts.token).await()
            }
        } catch (_: Exception) {
            // Fall through to cached location providers
        } finally {
            cts.cancel()
        }

        // 2. Fallback to Fused last known location
        if (resolvedLocation == null) {
            resolvedLocation = try {
                withTimeoutOrNull(1_500L) {
                    fusedClient.lastLocation.await()
                }
            } catch (_: Exception) {
                null
            }
        }

        // 3. Fallback to Android LocationManager providers
        if (resolvedLocation == null) {
            resolvedLocation = getLastKnownFromLocationManager(context)
        }

        if (resolvedLocation == null) {
            return GpsCoordinateFix(
                latitude = UNSET_LAT,
                longitude = UNSET_LNG,
                accuracyMeters = null,
                addressLabel = fallbackLabel,
                isLiveGps = false
            )
        }

        val lat = resolvedLocation.latitude
        val lng = resolvedLocation.longitude
        val accuracy = if (resolvedLocation.hasAccuracy()) resolvedLocation.accuracy else null
        val streetAddress = reverseGeocodeAddress(context, lat, lng)
        val coordSummary = formatCoordinates(lat, lng)
        val fullLabel = if (!streetAddress.isNullOrBlank()) {
            "$streetAddress ($coordSummary)"
        } else {
            "Live GPS ($coordSummary)"
        }

        return GpsCoordinateFix(
            latitude = lat,
            longitude = lng,
            accuracyMeters = accuracy,
            addressLabel = fullLabel,
            isLiveGps = true
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownFromLocationManager(context: Context): Location? {
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            when {
                gps != null && network != null -> if (gps.time >= network.time) gps else network
                gps != null -> gps
                else -> network
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun reverseGeocodeAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(2_500L) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                if (continuation.isActive) continuation.resume(addresses)
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(emptyList())
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)
            }
            val addr = addresses?.firstOrNull()
            listOfNotNull(addr?.thoroughfare, addr?.subThoroughfare, addr?.locality)
                .joinToString(", ")
                .ifBlank { addr?.getAddressLine(0) }
        } catch (_: Exception) {
            null
        }
    }
}
