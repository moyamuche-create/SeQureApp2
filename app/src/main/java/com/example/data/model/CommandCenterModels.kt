package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@IgnoreExtraProperties
data class CommandCenterTier(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val subtitle: String = "",
    val startingRate: String = "",
    val highlights: String = "",
    @get:PropertyName("isRecommended")
    @field:PropertyName("isRecommended")
    val isRecommended: Boolean = false,
    @get:PropertyName("isActive")
    @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommandCenterService(
    @DocumentId val id: String = "",
    val code: String = "",
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val tierCode: String = "",
    val tierTitle: String = "",
    val hourlyRate: Double? = null,
    val priceLabel: String = "",
    val badgeLabel: String = "",
    @get:PropertyName("isArmed")
    @field:PropertyName("isArmed")
    val isArmed: Boolean = false,
    @get:PropertyName("hasGpsTracking")
    @field:PropertyName("hasGpsTracking")
    val hasGpsTracking: Boolean = true,
    @get:PropertyName("isRecommended")
    @field:PropertyName("isRecommended")
    val isRecommended: Boolean = false,
    @get:PropertyName("isEmergencyDefault")
    @field:PropertyName("isEmergencyDefault")
    val isEmergencyDefault: Boolean = false,
    @get:PropertyName("isActive")
    @field:PropertyName("isActive")
    val isActive: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val resolvedTierTitle: String
        get() = tierTitle.ifBlank {
            tierCode.lowercase().replaceFirstChar { it.uppercase() }
        }

    fun estimateTotalLabel(hours: Long): String {
        val rate = hourlyRate
        if (rate != null && rate > 0.0) {
            val total = rate * hours.coerceAtLeast(1L)
            val totalFormatted = if (total % 1.0 == 0.0) {
                "$${total.toInt()}"
            } else {
                String.format(Locale.US, "$%.2f", total)
            }
            val ratePart = priceLabel.ifBlank {
                if (rate % 1.0 == 0.0) "$${rate.toInt()}/hr" else String.format(Locale.US, "$%.2f/hr", rate)
            }
            return "$totalFormatted est. ($ratePart)"
        }
        return priceLabel.ifBlank { "Quoted by Command Center" }
    }
}

@IgnoreExtraProperties
data class CommandCenterDeployment(
    @DocumentId val id: String = "",
    val clientUserId: String = "",
    val requestId: String = "",
    val guardName: String = "",
    val badgeNumber: String = "",
    val serviceType: String = "",
    val phone: String = "",
    val vehicle: String = "",
    val bio: String = "",
    val yearsOfExperience: Int = 0,
    val rating: Double = 0.0,
    val completedMissions: Int = 0,
    val specializations: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val guardLat: Double? = null,
    val guardLng: Double? = null,
    val status: String = "DISPATCHED",
    @get:PropertyName("isRecommended")
    @field:PropertyName("isRecommended")
    val isRecommended: Boolean = false,
    @get:PropertyName("isActive")
    @field:PropertyName("isActive")
    val isActive: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CommandCenterCatalog(
    val tiers: List<CommandCenterTier> = emptyList(),
    val services: List<CommandCenterService> = emptyList()
) {
    val isEmpty: Boolean
        get() = services.isEmpty()

    fun servicesForTier(tierCode: String?): List<CommandCenterService> {
        if (tierCode.isNullOrBlank()) return services
        return services.filter { it.tierCode.equals(tierCode, ignoreCase = true) }
    }

    fun findTierForService(service: CommandCenterService): CommandCenterTier? {
        return tiers.firstOrNull { it.code.equals(service.tierCode, ignoreCase = true) }
    }

    fun findEmergencyService(): CommandCenterService? {
        return services.firstOrNull { it.isEmergencyDefault }
            ?: services.firstOrNull {
                it.code.contains("SOS", ignoreCase = true) ||
                    it.code.contains("EMERGENCY", ignoreCase = true) ||
                    it.code.contains("RESPONSE", ignoreCase = true)
            }
    }
}
