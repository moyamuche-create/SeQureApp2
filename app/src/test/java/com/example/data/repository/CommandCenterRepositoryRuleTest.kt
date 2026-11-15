package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.CommandCenterService
import com.example.data.model.CommandCenterTier
import com.example.data.model.SecurityRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class CommandCenterRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private suspend fun promoteAdminViaEmulatorRest(adminUid: String) = withContext(Dispatchers.IO) {
        val url = URL(
            "http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$adminUid"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer owner")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 3000
            readTimeout = 3000
        }
        val body = """{"fields":{"active":{"booleanValue":true},"role":{"stringValue":"dispatcher"}}}"""
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val responseCode = conn.responseCode
        val errorText = if (responseCode !in 200..299 && responseCode != 409) {
            conn.errorStream?.bufferedReader()?.readText().orEmpty()
        } else ""
        conn.disconnect()
        check(responseCode in 200..299 || responseCode == 409) {
            "Failed to seed /admins/$adminUid in emulator: HTTP $responseCode $errorText"
        }
    }

    @Test
    fun observeCommandCenterCatalog_returnsOnlyActiveTiersAndServices() = runBlocking {
        val adminUid = signInTestUser(ADMIN_EMAIL)
        promoteAdminViaEmulatorRest(adminUid)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection(SecurityRepository.COLLECTION_TIERS).document("tier_active")
                .set(
                    CommandCenterTier(
                        code = "PROPERTY",
                        title = "Property Defense",
                        subtitle = "Command-managed perimeter defense",
                        startingRate = "From $45/hr",
                        isRecommended = true,
                        isActive = true,
                        sortOrder = 1L
                    )
                ).await()
            firestore.collection(SecurityRepository.COLLECTION_TIERS).document("tier_inactive")
                .set(
                    CommandCenterTier(
                        code = "ARCHIVED",
                        title = "Archived Tier",
                        isActive = false,
                        sortOrder = 2L
                    )
                ).await()

            firestore.collection(SecurityRepository.COLLECTION_SERVICES).document("svc_active")
                .set(
                    CommandCenterService(
                        code = "ARMED_PATROL",
                        title = "Armed Patrol",
                        subtitle = "Mobile deterrent",
                        tierCode = "PROPERTY",
                        tierTitle = "Property Defense",
                        hourlyRate = 65.0,
                        priceLabel = "$65/hr",
                        isArmed = true,
                        isRecommended = true,
                        isActive = true,
                        sortOrder = 1L
                    )
                ).await()
            firestore.collection(SecurityRepository.COLLECTION_SERVICES).document("svc_inactive")
                .set(
                    CommandCenterService(
                        code = "OLD_SERVICE",
                        title = "Retired Patrol",
                        tierCode = "PROPERTY",
                        priceLabel = "$25/hr",
                        isActive = false,
                        sortOrder = 2L
                    )
                ).await()
        }

        signInTestUser(CLIENT_A_EMAIL)
        val repository = SecurityRepository(firestore)

        val catalog = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeCommandCenterCatalog().first()
        }
        assertTrue(catalog.tiers.any { it.id == "tier_active" && it.isRecommended })
        assertFalse(catalog.tiers.any { it.id == "tier_inactive" })
        assertTrue(catalog.services.any { it.id == "svc_active" && it.priceLabel == "$65/hr" })
        assertFalse(catalog.services.any { it.id == "svc_inactive" })
    }

    @Test
    fun createSecurityRequest_andCommandCenterDispatchUpdate_syncsBothCollections() = runBlocking {
        val adminUid = signInTestUser(ADMIN_EMAIL)
        promoteAdminViaEmulatorRest(adminUid)

        val clientUid = signInTestUser(CLIENT_A_EMAIL)
        val clientRepo = SecurityRepository(firestore)

        val request = SecurityRequest(
            securityTier = "Property",
            serviceType = "ARMED_PATROL",
            serviceName = "Armed Patrol",
            locationName = "500 Market St, San Francisco",
            latitude = 37.7905,
            longitude = -122.4015,
            durationHours = 4L,
            hourlyRate = 65.0,
            priceLabel = "$65/hr",
            estimatedTotalLabel = "$260 est. ($65/hr)"
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientRepo.createSecurityRequest(request)
        }
        assertTrue("Expected createSecurityRequest to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val dispatchId = createResult.getOrThrow()

        // Verify Command Center queue doc was written with REQUESTED and zero client-assigned guard
        val dispatchSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection(SecurityRepository.COLLECTION_DISPATCHES).document(dispatchId).get().await()
        }
        assertTrue(dispatchSnap.exists())
        assertEquals("REQUESTED", dispatchSnap.getString("status"))
        assertEquals(clientUid, dispatchSnap.getString("userId"))
        assertEquals(null, dispatchSnap.getString("guardName"))

        // Standalone Command Center admin assigns officer and advances status to EN_ROUTE
        signInTestUser(ADMIN_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection(SecurityRepository.COLLECTION_DEPLOYMENTS).document("dep_cc_1")
                .set(
                    CommandCenterDeployment(
                        clientUserId = clientUid,
                        requestId = dispatchId,
                        guardName = "Officer Elena Rostova",
                        badgeNumber = "CC-404",
                        serviceType = "Armed Patrol",
                        phone = "+1-555-0404",
                        vehicle = "Tactical SUV 04",
                        yearsOfExperience = 7,
                        rating = 4.9,
                        completedMissions = 88,
                        guardLat = 37.7918,
                        guardLng = -122.4002,
                        status = "EN_ROUTE",
                        isRecommended = true,
                        isActive = true
                    )
                ).await()

            firestore.collection(SecurityRepository.COLLECTION_DISPATCHES).document(dispatchId)
                .update(
                    mapOf(
                        "status" to "EN_ROUTE",
                        "guardName" to "Officer Elena Rostova",
                        "guardBadge" to "CC-404",
                        "guardPhone" to "+1-555-0404",
                        "guardVehicle" to "Tactical SUV 04",
                        "deploymentId" to "dep_cc_1",
                        "guardLat" to 37.7918,
                        "guardLng" to -122.4002,
                        "etaMinutes" to 4L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }

        // Client observes live dispatch + deployment from Command Center
        signInTestUser(CLIENT_A_EMAIL)
        val requests = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientRepo.observeUserRequests().first { list ->
                list.any { it.id == dispatchId && it.status == "EN_ROUTE" }
            }
        }
        val liveReq = requests.first { it.id == dispatchId }
        assertEquals("Officer Elena Rostova", liveReq.guardName)
        assertEquals("dep_cc_1", liveReq.deploymentId)
        assertEquals(4L, liveReq.etaMinutes)

        val deployments = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientRepo.observeUserDeployments().first { it.any { dep -> dep.id == "dep_cc_1" } }
        }
        assertEquals("Tactical SUV 04", deployments.first { it.id == "dep_cc_1" }.vehicle)

        // Owner can cancel their own dispatch
        val cancelRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientRepo.cancelRequest(dispatchId)
        }
        assertTrue("Expected cancelRequest to succeed: ${cancelRes.exceptionOrNull()}", cancelRes.isSuccess)
    }

    @Test
    fun crossUserIsolationAndUnauthenticatedRejection_enforcedByRules() = runBlocking {
        val adminUid = signInTestUser(ADMIN_EMAIL)
        promoteAdminViaEmulatorRest(adminUid)

        val clientAUid = signInTestUser(CLIENT_A_EMAIL)
        val clientARepo = SecurityRepository(firestore)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientARepo.createSecurityRequest(
                SecurityRequest(
                    serviceType = "SOS_EMERGENCY",
                    serviceName = "SOS Alert",
                    isEmergency = true,
                    locationName = "Pier 27",
                    latitude = 37.806,
                    longitude = -122.402
                )
            )
        }
        assertTrue(created.isSuccess)
        val clientADispatchId = created.getOrThrow()

        // Client B tries to cancel or tamper with Client A's dispatch -> PERMISSION_DENIED
        signInTestUser(CLIENT_B_EMAIL)
        val clientBRepo = SecurityRepository(firestore)
        val crossCancel = withTimeout(DEFAULT_TIMEOUT_MS) {
            clientBRepo.cancelRequest(clientADispatchId)
        }
        assertTrue(crossCancel.isFailure)
        val cancelException = crossCancel.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException on cross-user cancel", cancelException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, cancelException?.code)

        // Direct read of Client A's deployment by Client B fails with PERMISSION_DENIED
        signInTestUser(ADMIN_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection(SecurityRepository.COLLECTION_DEPLOYMENTS).document("dep_private_a")
                .set(
                    CommandCenterDeployment(
                        clientUserId = clientAUid,
                        requestId = clientADispatchId,
                        guardName = "Officer Private",
                        isActive = true
                    )
                ).await()
        }

        signInTestUser(CLIENT_B_EMAIL)
        val readPrivateError = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(SecurityRepository.COLLECTION_DEPLOYMENTS).document("dep_private_a").get().await()
            }
        }.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected PERMISSION_DENIED when Client B reads Client A deployment", readPrivateError)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, readPrivateError?.code)

        // Unauthenticated read fails fast cleanly
        auth.signOut()
        val unauthCatalogError = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                clientBRepo.observeCommandCenterCatalog().first()
            }
        }.exceptionOrNull()
        assertNotNull("Expected unauthenticated catalog observation to fail", unauthCatalogError)
    }

    private companion object {
        const val ADMIN_EMAIL = "cc_admin@test.com"
        const val CLIENT_A_EMAIL = "client_a_cc@test.com"
        const val CLIENT_B_EMAIL = "client_b_cc@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
