package com.example

import com.example.data.location.PanicLocationProvider
import com.example.data.model.CommandCenterCatalog
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.CommandCenterService
import com.example.data.model.CommandCenterTier
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest
import com.example.data.model.SecurityServiceType
import com.example.data.model.SecurityTier
import com.example.ui.PanicDispatchReceipt
import com.example.ui.dispatch.ScheduleDateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SecurityModelUnitTest {

    @Test
    fun testPanicLocationFormattingAndPayload() {
        assertEquals("37.77490, -122.41940", PanicLocationProvider.formatCoordinates(37.7749, -122.4194))

        val liveNotes = PanicLocationProvider.buildPanicDispatchNotes(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = 12.4f,
            isLiveGps = true,
            emergencyContact = "Sarah (+1-555-0987)"
        )
        assertTrue(liveNotes.contains("HIGH-PRIORITY PANIC ALERT"))
        assertTrue(liveNotes.contains("LIVE GPS FIX (±12m)"))
        assertTrue(liveNotes.contains("37.77490, -122.41940"))
        assertTrue(liveNotes.contains("ICE: Sarah (+1-555-0987)"))

        val fallbackNotes = PanicLocationProvider.buildPanicDispatchNotes(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = null,
            isLiveGps = false,
            emergencyContact = null
        )
        assertTrue(fallbackNotes.contains("LAST KNOWN / SAVED SITE"))
        assertFalse(fallbackNotes.contains("ICE:"))

        val receipt = PanicDispatchReceipt(
            requestId = "req_911",
            locationLabel = "Market St (37.77490, -122.41940)",
            latitude = 37.7749,
            longitude = -122.4194,
            isLiveGps = true,
            accuracyMeters = 10f,
            assignedUnit = null,
            etaMinutes = null,
            emergencyContactName = "Sarah Carter",
            emergencyContactPhone = "+1-555-0987",
            emergencyContactSummary = "Sarah Carter (Spouse) • +1-555-0987",
            emergencyAlertMessage = PanicLocationProvider.buildEmergencyContactAlertMessage(
                senderName = "Alex Carter",
                emergencyContactName = "Sarah Carter",
                latitude = 37.7749,
                longitude = -122.4194,
                locationLabel = "Market St (37.77490, -122.41940)",
                isLiveGps = true,
                accuracyMeters = 10f
            )
        )
        assertEquals("req_911", receipt.requestId)
        assertTrue(receipt.isLiveGps)
        assertNull(receipt.assignedUnit)
        assertNull(receipt.etaMinutes)
        assertEquals("+1-555-0987", receipt.emergencyContactPhone)
        assertTrue(receipt.emergencyAlertMessage.contains("SOS EMERGENCY ALERT from Alex Carter!"))
        assertTrue(receipt.emergencyAlertMessage.contains("Sarah Carter, I need immediate assistance."))
        assertTrue(receipt.emergencyAlertMessage.contains("Live GPS (±10m)"))
        assertTrue(receipt.emergencyAlertMessage.contains("https://maps.google.com/?q=37.77490,-122.41940"))
    }

    @Test
    fun testEmergencyContactAlertMessageFallbackAndMapsLink() {
        assertEquals(
            "https://maps.google.com/?q=34.05223,-118.24368",
            PanicLocationProvider.buildGoogleMapsLink(34.05223, -118.24368)
        )

        val fallbackMsg = PanicLocationProvider.buildEmergencyContactAlertMessage(
            senderName = null,
            emergencyContactName = null,
            latitude = 34.05223,
            longitude = -118.24368,
            locationLabel = "Registered Site",
            isLiveGps = false,
            accuracyMeters = null
        )
        assertTrue(fallbackMsg.contains("SOS EMERGENCY ALERT from SeQure User!"))
        assertTrue(fallbackMsg.contains("Last Known Site"))
        assertTrue(fallbackMsg.contains("34.05223, -118.24368"))
        assertTrue(fallbackMsg.contains("https://maps.google.com/?q=34.05223,-118.24368"))

        val unsetCoordMsg = PanicLocationProvider.buildEmergencyContactAlertMessage(
            senderName = "Alex",
            emergencyContactName = "Sarah",
            latitude = PanicLocationProvider.UNSET_LAT,
            longitude = PanicLocationProvider.UNSET_LNG,
            locationLabel = "Registered Site (GPS Unavailable)",
            isLiveGps = false,
            accuracyMeters = null
        )
        assertTrue(unsetCoordMsg.contains("Unavailable (device location pending)"))
        assertFalse(unsetCoordMsg.contains("maps.google.com"))
    }

    @Test
    fun testCommandCenterCatalogFilteringAndPricing() {
        val personalTier = CommandCenterTier(
            id = "tier_personal",
            code = "PERSONAL",
            title = "Personal",
            subtitle = "Executive & mobile escorts",
            startingRate = "From $75/hr",
            isRecommended = false,
            isActive = true,
            sortOrder = 1L
        )
        val propertyTier = CommandCenterTier(
            id = "tier_property",
            code = "PROPERTY",
            title = "Property",
            subtitle = "Static site & asset defense",
            startingRate = "From $35/hr",
            isRecommended = true,
            isActive = true,
            sortOrder = 2L
        )
        val standardService = CommandCenterService(
            id = "svc_standard",
            code = "STANDARD_GUARD",
            title = "Standard Guard",
            tierCode = "PROPERTY",
            tierTitle = "Property",
            hourlyRate = 40.0,
            priceLabel = "$40/hr",
            isRecommended = false,
            isActive = true,
            sortOrder = 1L
        )
        val armedResponse = CommandCenterService(
            id = "svc_sos",
            code = "ARMED_RESPONSE",
            title = "Armed Response",
            tierCode = "PERSONAL",
            tierTitle = "Personal",
            hourlyRate = null,
            priceLabel = "$50/mo + Callout",
            isArmed = true,
            isRecommended = true,
            isEmergencyDefault = true,
            isActive = true,
            sortOrder = 2L
        )

        val catalog = CommandCenterCatalog(
            tiers = listOf(personalTier, propertyTier),
            services = listOf(standardService, armedResponse)
        )

        assertFalse(catalog.isEmpty)
        assertEquals(2, catalog.servicesForTier(null).size)
        assertEquals(1, catalog.servicesForTier("PROPERTY").size)
        assertEquals("STANDARD_GUARD", catalog.servicesForTier("property").first().code)
        assertEquals("tier_property", catalog.findTierForService(standardService)?.id)
        assertEquals("ARMED_RESPONSE", catalog.findEmergencyService()?.code)

        assertEquals("$160 est. ($40/hr)", standardService.estimateTotalLabel(4L))
        assertEquals("$50/mo + Callout", armedResponse.estimateTotalLabel(4L))
        assertEquals(
            "Quoted by Command Center",
            CommandCenterService(code = "CUSTOM", title = "Custom", tierCode = "EVENT").estimateTotalLabel(2L)
        )
    }

    @Test
    fun testAllRequiredServiceTypesExist() {
        val types = SecurityServiceType.entries
        assertEquals(7, types.size)

        assertNotNull(SecurityServiceType.fromCode("STANDARD_GUARD"))
        assertNotNull(SecurityServiceType.fromCode("EVENT_SECURITY"))
        assertNotNull(SecurityServiceType.fromCode("ARMED_GUARD"))
        assertNotNull(SecurityServiceType.fromCode("BODYGUARD"))
        assertNotNull(SecurityServiceType.fromCode("CASH_IN_TRANSIT"))
        assertNotNull(SecurityServiceType.fromCode("ARMED_RESPONSE"))
        assertNotNull(SecurityServiceType.fromCode("ESCORT_GPS"))
    }

    @Test
    fun testScheduleDateTimeFormatter() {
        val today = ScheduleDateTimeFormatter.todayUtcMidnightMillis()
        val tomorrow = ScheduleDateTimeFormatter.tomorrowUtcMidnightMillis()
        assertTrue(tomorrow > today)

        val fixedUtcMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 15, 0, 0, 0)
        }.timeInMillis

        assertEquals("Sun, Nov 15, 2026", ScheduleDateTimeFormatter.formatDate(fixedUtcMillis))
        assertEquals("Nov 15, 2026", ScheduleDateTimeFormatter.formatShortDate(fixedUtcMillis))
        assertEquals("9:30 AM", ScheduleDateTimeFormatter.formatTime(9, 30))
        assertEquals(
            "Sun, Nov 15, 2026 at 9:30 AM",
            ScheduleDateTimeFormatter.formatFullStartDateTime(fixedUtcMillis, 9, 30)
        )
        assertEquals("1:30 PM", ScheduleDateTimeFormatter.formatEndTime(9, 30, 4L))
    }

    @Test
    fun testSecurityRequestStateTransitionsAndPricingSummary() {
        val activeRequest = SecurityRequest(
            securityTier = "Property",
            serviceType = "ARMED_GUARD",
            serviceName = "Armed Guard",
            status = "EN_ROUTE",
            isEmergency = true,
            durationHours = 4L,
            hourlyRate = 65.0,
            priceLabel = "$65/hr"
        )
        assertTrue(activeRequest.isActive)
        assertFalse(activeRequest.isCompleted)
        assertFalse(activeRequest.isCancelled)
        assertEquals(SecurityTier.PROPERTY, activeRequest.tierEnum)
        assertEquals("Property", activeRequest.resolvedTierDisplay)
        assertEquals("$260 est. ($65/hr)", activeRequest.resolvedPriceSummary())

        val quotedRequest = SecurityRequest(
            serviceType = "SOS_EMERGENCY",
            serviceName = "SOS Alert",
            estimatedTotalLabel = "$120 Callout"
        )
        assertEquals("$120 Callout", quotedRequest.resolvedPriceSummary())

        val completedRequest = activeRequest.copy(status = "COMPLETED")
        assertFalse(completedRequest.isActive)
        assertTrue(completedRequest.isCompleted)
    }

    @Test
    fun testSecurityGuardProfileBuiltOnlyFromCommandCenterData() {
        val unassignedRequest = SecurityRequest(
            id = "req_unassigned",
            serviceType = "STANDARD_GUARD",
            serviceName = "Standard Guard",
            status = "REQUESTED"
        )
        assertNull(SecurityGuardProfile.fromCommandCenter(unassignedRequest, null))

        val assignedRequest = unassignedRequest.copy(
            status = "DISPATCHED",
            guardName = "Officer Elena Rostova",
            guardBadge = "CC-404",
            guardPhone = "+1-555-0404",
            guardVehicle = "Unit-04",
            hourlyRate = 55.0
        )
        val deployment = CommandCenterDeployment(
            id = "dep_404",
            clientUserId = "user_1",
            requestId = "req_unassigned",
            guardName = "Officer Elena Rostova",
            badgeNumber = "CC-404",
            serviceType = "Property Patrol",
            phone = "+1-555-0404",
            vehicle = "Patrol SUV CC-04",
            bio = "Command Center rapid tactical lead.",
            yearsOfExperience = 9,
            rating = 4.9,
            completedMissions = 142,
            specializations = listOf("Perimeter Defense", "Rapid Intercept"),
            certifications = listOf("BSIS Armed", "EMT-B"),
            isRecommended = true,
            isActive = true
        )

        val profile = SecurityGuardProfile.fromCommandCenter(assignedRequest, deployment)
        assertNotNull(profile)
        assertEquals("dep_404", profile?.id)
        assertEquals("Officer Elena Rostova", profile?.name)
        assertEquals("ER", profile?.avatarInitials)
        assertEquals("CC-404", profile?.badgeNumber)
        assertEquals("Patrol SUV CC-04", profile?.vehicle)
        assertEquals(9, profile?.yearsOfExperience)
        assertEquals(4.9f, profile?.rating ?: 0f, 0.01f)
        assertEquals(142, profile?.completedMissions)
        assertTrue(profile?.isRecommended == true)
        assertEquals(55.0, profile?.hourlyRate ?: 0.0, 0.01)
    }

    @Test
    fun testSecurityRequestFeedback() {
        val completedRequest = SecurityRequest(
            serviceType = "STANDARD_GUARD",
            serviceName = "Standard Guard",
            status = "COMPLETED",
            guardName = "Officer Elena Rostova",
            rating = 5,
            feedbackComment = "Prompt arrival and exemplary patrol service."
        )
        assertTrue(completedRequest.isCompleted)
        assertEquals(5, completedRequest.rating)
        assertEquals("Prompt arrival and exemplary patrol service.", completedRequest.feedbackComment)
    }

    @Test
    fun testUserProfilePreferences() {
        val homePreferred = com.example.data.model.UserProfile(
            userId = "user_123",
            displayName = "Alex Carter",
            email = "alex.carter@example.com",
            phoneNumber = "+1 (555) 012-3456",
            emergencyContactName = "Sarah Carter",
            emergencyContactPhone = "+1-555-0987",
            emergencyContactRelation = "Spouse",
            homeAddress = "420 Oak Ave, San Francisco, CA",
            businessAddress = "500 Howard St, San Francisco, CA",
            preferredAddressType = com.example.data.model.UserProfile.ADDRESS_TYPE_HOME
        )
        assertEquals("Alex Carter", homePreferred.displayName)
        assertEquals("Sarah Carter (Spouse) • +1-555-0987", homePreferred.effectiveEmergencyContactSummary)
        assertEquals("420 Oak Ave, San Francisco, CA", homePreferred.resolvedDefaultAddress)
        assertTrue(homePreferred.hasEmergencyContactConfigured)
        assertTrue(homePreferred.hasSavedAddressConfigured)

        val businessPreferred = homePreferred.copy(
            preferredAddressType = com.example.data.model.UserProfile.ADDRESS_TYPE_BUSINESS
        )
        assertEquals("500 Howard St, San Francisco, CA", businessPreferred.resolvedDefaultAddress)
    }

    @Test
    fun testDispatchNotificationCopyForStartAndArrival() {
        val startedCopy = com.example.service.DispatchNotificationHelper.buildStatusNotificationCopy(
            status = "DISPATCHED",
            serviceName = "Armed Guard",
            guardName = "Officer Elena Rostova",
            locationName = "500 Market St",
            etaMinutes = 8L
        )
        assertTrue(startedCopy.title.contains("Security Service Started"))
        assertTrue(startedCopy.body.contains("Officer Elena Rostova"))
        assertTrue(startedCopy.body.contains("500 Market St"))
        assertTrue(startedCopy.body.contains("8 min"))

        val arrivedCopy = com.example.service.DispatchNotificationHelper.buildStatusNotificationCopy(
            status = "ON_SITE",
            serviceName = "Executive Bodyguard",
            guardName = "Captain Elena Rostova",
            locationName = "420 Oak Ave"
        )
        assertTrue(arrivedCopy.title.contains("Guard Arrived at Location"))
        assertTrue(arrivedCopy.body.contains("Captain Elena Rostova"))
        assertTrue(arrivedCopy.body.contains("420 Oak Ave"))

        val customFcmCopy = com.example.service.DispatchNotificationHelper.buildStatusNotificationCopy(
            status = "EN_ROUTE",
            serviceName = "Event Security",
            explicitTitle = "Command Update",
            explicitBody = "Custom FCM body override"
        )
        assertEquals("Command Update", customFcmCopy.title)
        assertEquals("Custom FCM body override", customFcmCopy.body)
    }

    @Test
    fun testRadarTelemetryCalculatorCalculations() {
        val request = SecurityRequest(
            id = "req_radar_1",
            serviceType = "ARMED_GUARD",
            serviceName = "Armed Guard",
            status = "EN_ROUTE",
            latitude = 37.7955,
            longitude = -122.3937,
            guardLat = 37.7920,
            guardLng = -122.3980,
            etaMinutes = 3L
        )

        val snapshot = com.example.ui.tracking.RadarTelemetryCalculator.computeSnapshot(
            request = request,
            rangeMeters = 1000.0
        )
        assertTrue(snapshot.hasGuardCoordinates)
        assertTrue(snapshot.distanceMeters in 450.0..650.0)
        assertTrue(snapshot.normalizedRadius in 0.40f..0.70f)
        assertEquals("SW", com.example.ui.tracking.RadarTelemetryCalculator.compassCardinal(snapshot.bearingDegrees))
        assertTrue(snapshot.bearingCompassLabel.endsWith("SW"))
        assertTrue(snapshot.distanceLabel.endsWith(" m"))

        val pendingRequest = SecurityRequest(
            id = "req_radar_2",
            status = "REQUESTED",
            latitude = 37.7955,
            longitude = -122.3937
        )
        val pendingSnapshot = com.example.ui.tracking.RadarTelemetryCalculator.computeSnapshot(
            request = pendingRequest,
            rangeMeters = 3000.0
        )
        assertFalse(pendingSnapshot.hasGuardCoordinates)
        assertEquals("Syncing GPS", pendingSnapshot.bearingCompassLabel)
        assertEquals("2.55 km", pendingSnapshot.distanceLabel)

        assertEquals("120 m", com.example.ui.tracking.RadarTelemetryCalculator.formatDistanceMiles(120.0))
        assertEquals("1.00 mi", com.example.ui.tracking.RadarTelemetryCalculator.formatDistanceMiles(1609.344))
        assertEquals("Standby", com.example.ui.tracking.RadarTelemetryCalculator.formatStatusLabel(null))
        assertEquals("Dispatching", com.example.ui.tracking.RadarTelemetryCalculator.formatStatusLabel("REQUESTED"))
        assertEquals("En Route", com.example.ui.tracking.RadarTelemetryCalculator.formatStatusLabel("EN_ROUTE"))
        assertEquals("On Site", com.example.ui.tracking.RadarTelemetryCalculator.formatStatusLabel("ON_SITE"))
    }

    @Test
    fun testSosHoldProgressiveHapticSchedule() {
        val progresses = listOf(0.0f, 0.25f, 0.45f, 0.65f, 0.85f)
        val profiles = progresses.map {
            com.example.service.EmergencyNotificationHelper.computeHoldPulseSchedule(it)
        }
        assertEquals(listOf(0, 1, 2, 3, 4), profiles.map { it.stepIndex })

        for (i in 1 until profiles.size) {
            assertTrue(profiles[i].durationMs > profiles[i - 1].durationMs)
            assertTrue(profiles[i].amplitude > profiles[i - 1].amplitude)
        }
        assertEquals(255, profiles.last().amplitude)
    }

    @Test
    fun testCommandCenterRecommendedFlagsConfiguration() {
        val recommendedTier = CommandCenterTier(code = "PROPERTY", title = "Property", isRecommended = true)
        val regularTier = CommandCenterTier(code = "PERSONAL", title = "Personal", isRecommended = false)
        assertTrue(recommendedTier.isRecommended)
        assertFalse(regularTier.isRecommended)

        val recommendedSvc = CommandCenterService(
            code = "ARMED_GUARD",
            title = "Armed Guard",
            tierCode = "PROPERTY",
            isRecommended = true
        )
        val regularSvc = CommandCenterService(
            code = "STANDARD_GUARD",
            title = "Standard Guard",
            tierCode = "PROPERTY",
            isRecommended = false
        )
        assertTrue(recommendedSvc.isRecommended)
        assertFalse(regularSvc.isRecommended)
    }

    @Test
    fun testNearbyGuardsMapperDistanceSortingAndFiltering() {
        val userLat = 37.7749
        val userLng = -122.4194

        val fartherGuard = CommandCenterDeployment(
            id = "dep_far",
            requestId = "req_1",
            guardName = "Officer Marcus Vance",
            badgeNumber = "CC-202",
            serviceType = "Armed Guard",
            guardLat = 37.7950,
            guardLng = -122.3930,
            status = "DISPATCHED",
            isActive = true
        )
        val closerGuard = CommandCenterDeployment(
            id = "dep_near",
            requestId = "req_2",
            guardName = "Officer Elena Rostova",
            badgeNumber = "CC-101",
            serviceType = "Rapid Response",
            guardLat = 37.7760,
            guardLng = -122.4180,
            status = "EN_ROUTE",
            isRecommended = true,
            isActive = true
        )
        val unsetCoordsGuard = CommandCenterDeployment(
            id = "dep_unset",
            requestId = "req_3",
            guardName = "Pending Coords Unit",
            guardLat = 0.0,
            guardLng = 0.0,
            status = "DISPATCHED",
            isActive = true
        )
        val cancelledGuard = CommandCenterDeployment(
            id = "dep_cancelled",
            requestId = "req_4",
            guardName = "Cancelled Unit",
            guardLat = 37.7750,
            guardLng = -122.4190,
            status = "CANCELLED",
            isActive = false
        )

        val activeRequest = SecurityRequest(
            id = "req_2",
            deploymentId = "dep_near",
            serviceName = "Rapid Response",
            status = "EN_ROUTE",
            isEmergency = true,
            guardName = "Officer Elena Rostova",
            guardLat = 37.7760,
            guardLng = -122.4180,
            etaMinutes = 3L
        )

        val result = com.example.ui.tracking.NearbyGuardsMapper.buildNearbyGuards(
            deployments = listOf(fartherGuard, closerGuard, unsetCoordsGuard, cancelledGuard),
            activeRequest = activeRequest,
            activeDeployment = closerGuard,
            userLat = userLat,
            userLng = userLng
        )

        assertEquals(2, result.size)
        assertEquals("dep_near", result[0].id)
        assertTrue(result[0].isEmergency)
        assertEquals(3L, result[0].etaMinutes)
        assertEquals("dep_far", result[1].id)
        assertTrue((result[0].distanceMeters ?: Double.MAX_VALUE) < (result[1].distanceMeters ?: 0.0))
    }

    @Test
    fun testDynamicEtaCalculation() {
        val distanceMeters = 1500.0 // 1.5 km (~3 mins at 500m/min)
        val etaMins = com.example.ui.tracking.RadarTelemetryCalculator.computeDynamicEtaMinutes(distanceMeters, "EN_ROUTE")
        assertTrue(etaMins in 3L..4L)

        val arrivedEta = com.example.ui.tracking.RadarTelemetryCalculator.computeDynamicEtaMinutes(distanceMeters, "ON_SITE")
        assertEquals(0L, arrivedEta)

        val formattedEta = com.example.ui.tracking.RadarTelemetryCalculator.formatDynamicEtaDisplay(distanceMeters, "EN_ROUTE", null)
        assertTrue(formattedEta.contains("mins"))

        val arrivedFormatted = com.example.ui.tracking.RadarTelemetryCalculator.formatDynamicEtaDisplay(null, "ON_SITE", null)
        assertEquals("Arrived on Site", arrivedFormatted)
    }
}
