package com.example.data.model

data class GuardReview(
    val reviewerName: String,
    val rating: Float,
    val date: String,
    val comment: String
)

data class SecurityGuardProfile(
    val id: String,
    val name: String,
    val badgeNumber: String,
    val serviceType: String,
    val yearsOfExperience: Int,
    val rating: Float,
    val reviewCount: Int,
    val bio: String,
    val specializations: List<String>,
    val certifications: List<String>,
    val vehicle: String,
    val completedMissions: Int,
    val hourlyRate: Double,
    val isAvailable: Boolean = true,
    val avatarInitials: String = "",
    val phone: String = "",
    val isRecommended: Boolean = false,
    val reviews: List<GuardReview> = emptyList()
) {
    companion object {
        /**
         * Builds a live officer profile exclusively from Command Center deployment/dispatch records.
         * Returns null when no officer has been assigned by the Command Center yet (zero demo data).
         */
        fun fromCommandCenter(
            request: SecurityRequest?,
            deployment: CommandCenterDeployment?
        ): SecurityGuardProfile? {
            val resolvedName = deployment?.guardName?.takeIf { it.isNotBlank() }
                ?: request?.guardName?.takeIf { it.isNotBlank() }
                ?: return null

            val initials = resolvedName
                .split(" ")
                .filter { it.isNotBlank() }
                .takeLast(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifBlank { "SO" }

            return SecurityGuardProfile(
                id = deployment?.id?.ifBlank { request?.deploymentId.orEmpty() }
                    ?: request?.deploymentId
                    ?: request?.id
                    ?: "",
                name = resolvedName,
                badgeNumber = deployment?.badgeNumber?.ifBlank { request?.guardBadge.orEmpty() }
                    ?: request?.guardBadge
                    ?: "Verified",
                serviceType = deployment?.serviceType?.ifBlank { request?.serviceName.orEmpty() }
                    ?: request?.serviceName
                    ?: "Assigned Security Unit",
                yearsOfExperience = deployment?.yearsOfExperience ?: 0,
                rating = (deployment?.rating ?: 0.0).toFloat(),
                reviewCount = 0,
                bio = deployment?.bio?.ifBlank {
                    "Dispatched and monitored live via SeQure Command Center."
                } ?: "Dispatched and monitored live via SeQure Command Center.",
                specializations = deployment?.specializations.orEmpty(),
                certifications = deployment?.certifications.orEmpty(),
                vehicle = deployment?.vehicle?.ifBlank { request?.guardVehicle.orEmpty() }
                    ?: request?.guardVehicle
                    ?: "Command Center Patrol Unit",
                completedMissions = deployment?.completedMissions ?: 0,
                hourlyRate = request?.hourlyRate ?: 0.0,
                isAvailable = deployment?.isActive ?: (request?.isActive == true),
                avatarInitials = initials,
                phone = deployment?.phone?.ifBlank { request?.guardPhone.orEmpty() }
                    ?: request?.guardPhone.orEmpty(),
                isRecommended = deployment?.isRecommended ?: false,
                reviews = emptyList()
            )
        }
    }
}
