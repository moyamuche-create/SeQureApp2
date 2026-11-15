package com.example.ui.tracking

import com.example.data.model.CommandCenterDeployment
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest

/**
 * Represents a real Command Center security guard/unit plotted on [MapScreen].
 * Strictly backed by live Firestore deployments or active dispatch telemetry (zero demo data).
 */
data class NearbySecurityGuardMarker(
    val id: String,
    val requestId: String,
    val guardName: String,
    val badgeNumber: String,
    val serviceType: String,
    val vehicle: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double?,
    val rating: Double,
    val etaMinutes: Long?,
    val isRecommended: Boolean,
    val isEmergency: Boolean,
    val profile: SecurityGuardProfile?
) {
    val distanceLabel: String
        get() = when {
            distanceMeters != null -> RadarTelemetryCalculator.formatDistanceMiles(distanceMeters)
            status.equals("ON_SITE", ignoreCase = true) -> "On Site"
            else -> "Live GPS"
        }

    val statusLabel: String
        get() = RadarTelemetryCalculator.formatStatusLabel(status)
}

object NearbyGuardsMapper {

    fun hasValidCoordinates(lat: Double?, lng: Double?): Boolean {
        return lat != null && lng != null && (lat != 0.0 || lng != 0.0)
    }

    /**
     * Merges Command Center deployments and active request guard telemetry into a sorted,
     * deduplicated list of nearby security guard markers with geodesic distances from [userLat]/[userLng].
     */
    fun buildNearbyGuards(
        deployments: List<CommandCenterDeployment>,
        activeRequest: SecurityRequest?,
        activeDeployment: CommandCenterDeployment?,
        userLat: Double?,
        userLng: Double?
    ): List<NearbySecurityGuardMarker> {
        val hasUserLocation = hasValidCoordinates(userLat, userLng)
        val mergedDeployments = buildList {
            if (activeDeployment != null) add(activeDeployment)
            addAll(deployments)
        }.distinctBy { dep ->
            dep.id.ifBlank { "${dep.requestId}:${dep.badgeNumber}:${dep.guardName}" }
        }

        val items = mutableListOf<NearbySecurityGuardMarker>()
        val seenKeys = mutableSetOf<String>()

        for (dep in mergedDeployments) {
            if (!dep.isActive && dep.status.equals("CANCELLED", ignoreCase = true)) continue
            if (!dep.isActive && dep.status.equals("COMPLETED", ignoreCase = true)) continue
            if (!hasValidCoordinates(dep.guardLat, dep.guardLng)) continue

            val gLat = dep.guardLat!!
            val gLng = dep.guardLng!!
            val matchesActiveRequest = activeRequest != null && (
                dep.id == activeRequest.deploymentId ||
                    (dep.requestId.isNotBlank() && dep.requestId == activeRequest.id)
                )

            val dist = if (hasUserLocation) {
                RadarTelemetryCalculator.computeDistanceMeters(userLat!!, userLng!!, gLat, gLng)
            } else {
                null
            }

            val profile = SecurityGuardProfile.fromCommandCenter(
                request = if (matchesActiveRequest) activeRequest else null,
                deployment = dep
            )

            val key = dep.requestId.ifBlank { dep.id.ifBlank { "${dep.guardName}:$gLat:$gLng" } }
            if (seenKeys.add(key)) {
                items += NearbySecurityGuardMarker(
                    id = dep.id.ifBlank { key },
                    requestId = dep.requestId,
                    guardName = dep.guardName.ifBlank { "Assigned Security Unit" },
                    badgeNumber = dep.badgeNumber.ifBlank { "Verified" },
                    serviceType = dep.serviceType.ifBlank { "Security Patrol" },
                    vehicle = dep.vehicle.ifBlank { "Command Center Patrol Unit" },
                    status = dep.status.ifBlank { "DISPATCHED" },
                    latitude = gLat,
                    longitude = gLng,
                    distanceMeters = dist,
                    rating = dep.rating,
                    etaMinutes = if (matchesActiveRequest) activeRequest?.etaMinutes else null,
                    isRecommended = dep.isRecommended,
                    isEmergency = matchesActiveRequest && activeRequest?.isEmergency == true,
                    profile = profile
                )
            }
        }

        if (activeRequest != null &&
            activeRequest.isActive &&
            hasValidCoordinates(activeRequest.guardLat, activeRequest.guardLng)
        ) {
            val reqKey = activeRequest.id
            val alreadyRepresented = seenKeys.contains(reqKey) ||
                items.any { it.requestId == reqKey || (activeRequest.deploymentId != null && it.id == activeRequest.deploymentId) }

            if (!alreadyRepresented) {
                val gLat = activeRequest.guardLat!!
                val gLng = activeRequest.guardLng!!
                val dist = if (hasUserLocation) {
                    RadarTelemetryCalculator.computeDistanceMeters(userLat!!, userLng!!, gLat, gLng)
                } else {
                    null
                }
                val profile = SecurityGuardProfile.fromCommandCenter(activeRequest, activeDeployment)
                items += NearbySecurityGuardMarker(
                    id = activeRequest.deploymentId?.takeIf { it.isNotBlank() } ?: activeRequest.id,
                    requestId = activeRequest.id,
                    guardName = activeRequest.guardName?.takeIf { it.isNotBlank() } ?: "Assigned Unit",
                    badgeNumber = activeRequest.guardBadge?.takeIf { it.isNotBlank() } ?: "Verified",
                    serviceType = activeRequest.serviceName.ifBlank { "Security Detail" },
                    vehicle = activeRequest.guardVehicle?.takeIf { it.isNotBlank() } ?: "Command Center Patrol Unit",
                    status = activeRequest.status,
                    latitude = gLat,
                    longitude = gLng,
                    distanceMeters = dist,
                    rating = activeDeployment?.rating ?: 0.0,
                    etaMinutes = activeRequest.etaMinutes,
                    isRecommended = activeDeployment?.isRecommended == true,
                    isEmergency = activeRequest.isEmergency,
                    profile = profile
                )
            }
        }

        return items.sortedWith(
            compareBy<NearbySecurityGuardMarker>(
                { it.distanceMeters ?: Double.MAX_VALUE },
                { !it.isEmergency },
                { !it.isRecommended },
                { it.guardName }
            )
        )
    }
}
