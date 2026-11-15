package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val emergencyContact: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelation: String? = null,
    val homeAddress: String? = null,
    val businessAddress: String? = null,
    val preferredAddressType: String? = null,
    val defaultLocation: String? = null,
    val fcmToken: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val effectiveEmergencyContactSummary: String?
        get() = formatEmergencyContactSummary(
            name = emergencyContactName,
            phone = emergencyContactPhone,
            relation = emergencyContactRelation,
            legacySummary = emergencyContact
        )

    @get:Exclude
    val resolvedDefaultAddress: String?
        get() = when (preferredAddressType?.uppercase()) {
            "BUSINESS" -> businessAddress?.takeIf { it.isNotBlank() }
                ?: homeAddress?.takeIf { it.isNotBlank() }
                ?: defaultLocation?.takeIf { it.isNotBlank() }
            "HOME" -> homeAddress?.takeIf { it.isNotBlank() }
                ?: businessAddress?.takeIf { it.isNotBlank() }
                ?: defaultLocation?.takeIf { it.isNotBlank() }
            else -> defaultLocation?.takeIf { it.isNotBlank() }
                ?: homeAddress?.takeIf { it.isNotBlank() }
                ?: businessAddress?.takeIf { it.isNotBlank() }
        }

    @get:Exclude
    val hasEmergencyContactConfigured: Boolean
        get() = !effectiveEmergencyContactSummary.isNullOrBlank()

    @get:Exclude
    val hasSavedAddressConfigured: Boolean
        get() = !homeAddress.isNullOrBlank() || !businessAddress.isNullOrBlank()

    companion object {
        const val ADDRESS_TYPE_HOME = "HOME"
        const val ADDRESS_TYPE_BUSINESS = "BUSINESS"

        fun formatEmergencyContactSummary(
            name: String?,
            phone: String?,
            relation: String?,
            legacySummary: String? = null
        ): String? {
            val cleanName = name?.trim().orEmpty()
            val cleanPhone = phone?.trim().orEmpty()
            val cleanRel = relation?.trim().orEmpty()
            if (cleanName.isBlank() && cleanPhone.isBlank()) {
                return legacySummary?.trim()?.takeIf { it.isNotBlank() }
            }
            return buildString {
                if (cleanName.isNotBlank()) append(cleanName)
                if (cleanRel.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append("($cleanRel)")
                }
                if (cleanPhone.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(cleanPhone)
                }
            }.ifBlank { null }
        }
    }
}
