package com.example.data.model

enum class SecurityTier(
    val code: String,
    val title: String,
    val subtitle: String,
    val startingRate: String,
    val highlights: String,
    val isRecommended: Boolean = false
) {
    PERSONAL(
        code = "PERSONAL",
        title = "Personal",
        subtitle = "Bodyguards & GPS escorts",
        startingRate = "From $75/hr",
        highlights = "1-on-1 protection, executive travel & rapid response"
    ),
    PROPERTY(
        code = "PROPERTY",
        title = "Property",
        subtitle = "Homes, offices & assets",
        startingRate = "From $35/hr",
        highlights = "Static patrol, armed deterrence & armored transit",
        isRecommended = true
    ),
    EVENT(
        code = "EVENT",
        title = "Event",
        subtitle = "Venues & crowd control",
        startingRate = "From $45/hr",
        highlights = "Entry screening, VIP zones & perimeter management"
    );

    companion object {
        fun fromCode(code: String): SecurityTier {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PROPERTY
        }
    }
}

enum class SecurityServiceType(
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val hourlyRateEstimate: String,
    val hourlyRateValue: Int?,
    val badgeLabel: String,
    val tier: SecurityTier,
    val hasGpsTracking: Boolean,
    val isArmed: Boolean,
    val isRecommended: Boolean = false
) {
    STANDARD_GUARD(
        code = "STANDARD_GUARD",
        title = "Standard Guard",
        subtitle = "Static site & property protection",
        description = "Licensed security officer for access control, property surveillance, and visible deterrent.",
        hourlyRateEstimate = "$35/hr",
        hourlyRateValue = 35,
        badgeLabel = "Static Patrol",
        tier = SecurityTier.PROPERTY,
        hasGpsTracking = true,
        isArmed = false
    ),
    EVENT_SECURITY(
        code = "EVENT_SECURITY",
        title = "Event Security",
        subtitle = "Crowd management & venue safety",
        description = "Coordinated security teams for private gatherings, corporate events, ticket checkpoints, and perimeter security.",
        hourlyRateEstimate = "$45/hr",
        hourlyRateValue = 45,
        badgeLabel = "Crowd Control",
        tier = SecurityTier.EVENT,
        hasGpsTracking = true,
        isArmed = false,
        isRecommended = true
    ),
    ARMED_GUARD(
        code = "ARMED_GUARD",
        title = "Armed Guard",
        subtitle = "High-threat deterrent & tactical security",
        description = "Certified firearms-trained protection officers for high-value facilities, critical assets, and elevated risk zones.",
        hourlyRateEstimate = "$65/hr",
        hourlyRateValue = 65,
        badgeLabel = "Armed Tier-1",
        tier = SecurityTier.PROPERTY,
        hasGpsTracking = true,
        isArmed = true,
        isRecommended = true
    ),
    BODYGUARD(
        code = "BODYGUARD",
        title = "Close Protection / Bodyguard",
        subtitle = "Executive & VIP discreet escort",
        description = "Dedicated personal protection specialist trained in defensive tactics, evasive driving, and VIP escort protocols.",
        hourlyRateEstimate = "$95/hr",
        hourlyRateValue = 95,
        badgeLabel = "Executive VIP",
        tier = SecurityTier.PERSONAL,
        hasGpsTracking = true,
        isArmed = true
    ),
    CASH_IN_TRANSIT(
        code = "CASH_IN_TRANSIT",
        title = "Cash in Transit (CIT)",
        subtitle = "Armored logistics & bullion transport",
        description = "Armored security transport vehicle and dual-officer custody team for high-value physical asset relocation.",
        hourlyRateEstimate = "$140/hr",
        hourlyRateValue = 140,
        badgeLabel = "Armored Unit",
        tier = SecurityTier.PROPERTY,
        hasGpsTracking = true,
        isArmed = true
    ),
    ARMED_RESPONSE(
        code = "ARMED_RESPONSE",
        title = "Armed Response Basing",
        subtitle = "Rapid mobile patrol & alarm dispatch",
        description = "Dedicated tactical patrol vehicle stationed nearby for immediate 3-5 minute emergency response dispatch.",
        hourlyRateEstimate = "$50/mo + Callout",
        hourlyRateValue = null,
        badgeLabel = "Rapid Intercept",
        tier = SecurityTier.PERSONAL,
        hasGpsTracking = true,
        isArmed = true,
        isRecommended = true
    ),
    ESCORT_GPS(
        code = "ESCORT_GPS",
        title = "Personal Escort with GPS",
        subtitle = "Active satellite tracked safe travel",
        description = "Uniformed or plainclothes personal escort synchronized with continuous GPS telemetry and emergency panic button.",
        hourlyRateEstimate = "$75/hr",
        hourlyRateValue = 75,
        badgeLabel = "Active GPS Tracking",
        tier = SecurityTier.PERSONAL,
        hasGpsTracking = true,
        isArmed = false
    );

    fun estimateTotalLabel(hours: Long): String {
        val rate = hourlyRateValue ?: return hourlyRateEstimate
        return "$${rate * hours} est. ($hourlyRateEstimate)"
    }

    companion object {
        fun fromCode(code: String): SecurityServiceType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: STANDARD_GUARD
        }

        fun servicesForTier(tier: SecurityTier?): List<SecurityServiceType> {
            return if (tier == null) entries else entries.filter { it.tier == tier }
        }
    }
}
