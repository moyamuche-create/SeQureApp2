package com.example.ui.tracking

import com.example.data.model.SecurityRequest
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RadarTelemetrySnapshot(
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val normalizedRadius: Float,
    val distanceLabel: String,
    val bearingCompassLabel: String,
    val hasGuardCoordinates: Boolean
)

object RadarTelemetryCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun computeDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2.0) * sin(dLng / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun computeBearingDegrees(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Double {
        val phi1 = Math.toRadians(fromLat)
        val phi2 = Math.toRadians(toLat)
        val dLambda = Math.toRadians(toLng - fromLng)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return (theta + 360.0) % 360.0
    }

    fun compassCardinal(bearingDegrees: Double): String {
        val sectors = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((bearingDegrees % 360.0) + 360.0) % 360.0
        val index = (((normalized + 22.5) / 45.0).toInt()) % 8
        return sectors[index]
    }

    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000.0) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
        }
    }

    fun formatDistanceMiles(distanceMeters: Double): String {
        val miles = distanceMeters / 1609.344
        return if (miles < 0.1) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format(Locale.US, "%.2f mi", miles)
        }
    }

    fun formatStatusLabel(status: String?): String {
        return when (status?.uppercase()) {
            "REQUESTED" -> "Dispatching"
            "DISPATCHED" -> "Assigned"
            "EN_ROUTE" -> "En Route"
            "ON_SITE" -> "On Site"
            "COMPLETED" -> "Completed"
            "CANCELLED" -> "Cancelled"
            null, "", "STANDBY" -> "Standby"
            else -> status
        }
    }

    fun computeSnapshot(
        request: SecurityRequest,
        rangeMeters: Double
    ): RadarTelemetrySnapshot {
        val userLat = request.latitude
        val userLng = request.longitude
        val guardLat = request.guardLat
        val guardLng = request.guardLng

        val hasValidUser = userLat != 0.0 || userLng != 0.0
        val hasValidGuard = guardLat != null && guardLng != null && (guardLat != 0.0 || guardLng != 0.0)

        if (hasValidUser && hasValidGuard) {
            val dist = computeDistanceMeters(userLat, userLng, guardLat!!, guardLng!!)
            val bearing = computeBearingDegrees(userLat, userLng, guardLat, guardLng)
            val norm = (dist / rangeMeters.coerceAtLeast(100.0)).toFloat().coerceIn(0.08f, 0.94f)
            return RadarTelemetrySnapshot(
                distanceMeters = dist,
                bearingDegrees = bearing,
                normalizedRadius = if (request.status == "ON_SITE") 0.08f else norm,
                distanceLabel = formatDistance(dist),
                bearingCompassLabel = "${bearing.toInt()}° ${compassCardinal(bearing)}",
                hasGuardCoordinates = true
            )
        }

        // Derive realistic fallback distance from dispatch status / ETA when guard coordinates haven't arrived yet
        val fallbackDist = when (request.status) {
            "REQUESTED" -> rangeMeters * 0.85
            "DISPATCHED" -> ((request.etaMinutes ?: 6L) * 180.0).coerceAtMost(rangeMeters * 0.85)
            "EN_ROUTE" -> ((request.etaMinutes ?: 4L) * 140.0).coerceAtMost(rangeMeters * 0.7)
            "ON_SITE" -> 45.0
            else -> 0.0
        }
        val fallbackNorm = when (request.status) {
            "REQUESTED" -> 0.85f
            "DISPATCHED" -> (fallbackDist / rangeMeters).toFloat().coerceIn(0.25f, 0.82f)
            "EN_ROUTE" -> (fallbackDist / rangeMeters).toFloat().coerceIn(0.18f, 0.65f)
            "ON_SITE" -> 0.09f
            else -> 0.0f
        }

        return RadarTelemetrySnapshot(
            distanceMeters = fallbackDist,
            bearingDegrees = 315.0,
            normalizedRadius = fallbackNorm,
            distanceLabel = if (request.status == "COMPLETED" || request.status == "CANCELLED") "0 m" else formatDistance(fallbackDist),
            bearingCompassLabel = if (hasValidGuard) "315° NW" else "Syncing GPS",
            hasGuardCoordinates = false
        )
    }

    fun computeDynamicEtaMinutes(distanceMeters: Double, status: String?): Long {
        return when (status?.uppercase()) {
            "ON_SITE", "COMPLETED", "CANCELLED" -> 0L
            else -> {
                val speedMetersPerMin = 500.0 // ~30 km/h urban transit speed
                ((distanceMeters / speedMetersPerMin) + 1.0).toLong().coerceAtLeast(1L)
            }
        }
    }

    fun formatDynamicEtaDisplay(distanceMeters: Double?, status: String?, baseEtaMinutes: Long?): String {
        return when {
            status?.uppercase() == "ON_SITE" -> "Arrived on Site"
            status?.uppercase() == "COMPLETED" -> "Mission Completed"
            status?.uppercase() == "CANCELLED" -> "Cancelled"
            distanceMeters != null -> {
                val mins = computeDynamicEtaMinutes(distanceMeters, status)
                val targetMillis = System.currentTimeMillis() + (mins * 60000L)
                val clockTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(targetMillis))
                if (mins <= 1) "~1 min (~$clockTime)" else "~$mins mins (~$clockTime)"
            }
            baseEtaMinutes != null -> "~$baseEtaMinutes mins"
            else -> "Pending GPS"
        }
    }
}
