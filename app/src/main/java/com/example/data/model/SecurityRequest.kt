package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@IgnoreExtraProperties
data class SecurityRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val securityTier: String = "",
    val serviceType: String = "",
    val serviceName: String = "",
    val status: String = "REQUESTED", // REQUESTED, DISPATCHED, EN_ROUTE, ON_SITE, COMPLETED, CANCELLED
    @get:PropertyName("isEmergency")
    @field:PropertyName("isEmergency")
    val isEmergency: Boolean = false,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val targetDateTime: String = "",
    @get:PropertyName("isScheduled")
    @field:PropertyName("isScheduled")
    val isScheduled: Boolean = false,
    val durationHours: Long = 4L,
    val hourlyRate: Double? = null,
    val priceLabel: String? = null,
    val estimatedTotalLabel: String? = null,
    val notes: String? = null,
    val guardName: String? = null,
    val guardBadge: String? = null,
    val guardPhone: String? = null,
    val guardVehicle: String? = null,
    val deploymentId: String? = null,
    val guardLat: Double? = null,
    val guardLng: Double? = null,
    val etaMinutes: Long? = null,
    val rating: Int? = null,
    val feedbackComment: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val serviceTypeEnum: SecurityServiceType
        get() = SecurityServiceType.fromCode(serviceType)

    @get:Exclude
    val tierEnum: SecurityTier
        get() = if (securityTier.isNotBlank()) SecurityTier.fromCode(securityTier) else serviceTypeEnum.tier

    @get:Exclude
    val resolvedTierDisplay: String
        get() = securityTier.ifBlank { tierEnum.title }

    fun resolvedPriceSummary(): String {
        estimatedTotalLabel?.takeIf { it.isNotBlank() }?.let { return it }
        val rate = hourlyRate
        if (rate != null && rate > 0.0) {
            val total = rate * durationHours.coerceAtLeast(1L)
            val totalStr = if (total % 1.0 == 0.0) "$${total.toInt()}" else String.format(Locale.US, "$%.2f", total)
            val rateStr = priceLabel?.takeIf { it.isNotBlank() }
                ?: if (rate % 1.0 == 0.0) "$${rate.toInt()}/hr" else String.format(Locale.US, "$%.2f/hr", rate)
            return "$totalStr est. ($rateStr)"
        }
        return priceLabel?.takeIf { it.isNotBlank() } ?: "Command Center Quote"
    }

    @get:Exclude
    val isActive: Boolean
        get() = status in listOf("REQUESTED", "DISPATCHED", "EN_ROUTE", "ON_SITE")

    @get:Exclude
    val isCompleted: Boolean
        get() = status == "COMPLETED"

    @get:Exclude
    val isCancelled: Boolean
        get() = status == "CANCELLED"
}
