package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndFetchProfile_authenticatedOwner_persistsEmergencyContactAndAddresses() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SecurityRepository(firestore)

        val initialProfile = UserProfile(
            userId = uid,
            displayName = "Alice Mercer",
            email = ALICE_EMAIL,
            phoneNumber = "+1-555-0101",
            emergencyContactName = "Sarah Mercer",
            emergencyContactPhone = "+1-555-0199",
            emergencyContactRelation = "Spouse",
            homeAddress = "420 Oak Ave, San Francisco, CA",
            businessAddress = "101 California St, Suite 400",
            preferredAddressType = UserProfile.ADDRESS_TYPE_HOME
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(initialProfile)
        }
        assertTrue("Expected initial profile save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile()
        }
        assertTrue("Expected getUserProfile to succeed", fetchedResult.isSuccess)
        val fetched = fetchedResult.getOrNull()
        assertNotNull(fetched)
        assertEquals("Sarah Mercer", fetched?.emergencyContactName)
        assertEquals("+1-555-0199", fetched?.emergencyContactPhone)
        assertEquals("Spouse", fetched?.emergencyContactRelation)
        assertEquals("420 Oak Ave, San Francisco, CA", fetched?.homeAddress)
        assertEquals("101 California St, Suite 400", fetched?.businessAddress)
        assertEquals("420 Oak Ave, San Francisco, CA", fetched?.resolvedDefaultAddress)

        // Update preferred address to BUSINESS
        val updatedProfile = initialProfile.copy(preferredAddressType = UserProfile.ADDRESS_TYPE_BUSINESS)
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(updatedProfile)
        }
        assertTrue("Expected profile update to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val refetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile().getOrNull()
        }
        assertEquals(UserProfile.ADDRESS_TYPE_BUSINESS, refetched?.preferredAddressType)
        assertEquals("101 California St, Suite 400", refetched?.resolvedDefaultAddress)
    }

    @Test
    fun saveProfile_unauthenticatedUser_failsFast() = runBlocking {
        auth.signOut()
        val repository = SecurityRepository(firestore)

        val profile = UserProfile(
            displayName = "Unauth Caller",
            email = ALICE_EMAIL,
            homeAddress = "123 Main St"
        )
        val saveResult = repository.saveUserProfile(profile)
        assertTrue(saveResult.isFailure)

        val tokenResult = repository.saveFcmToken("unauthenticated_token_attempt")
        assertTrue(tokenResult.isFailure)
    }

    @Test
    fun saveFcmToken_authenticatedOwner_persistsTokenToFirestore() = runBlocking {
        // 1. Fresh user without an existing profile document creates doc with fcmToken
        val freshUid = signInTestUser(FCM_FRESH_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(freshUid).delete().await()
        }
        val repository = SecurityRepository(firestore)

        val freshTokenResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveFcmToken("fcm_fresh_token_123456")
        }
        assertTrue("Expected fresh saveFcmToken to succeed: ${freshTokenResult.exceptionOrNull()}", freshTokenResult.isSuccess)
        val freshFetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile().getOrNull()
        }
        assertEquals("fcm_fresh_token_123456", freshFetched?.fcmToken)

        // 2. Existing user with saved profile updates fcmToken without losing emergency contact or addresses
        val aliceUid = signInTestUser(ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid).delete().await()
        }
        val seedProfile = UserProfile(
            userId = aliceUid,
            displayName = "Alice Mercer",
            email = ALICE_EMAIL,
            emergencyContactName = "Sarah Mercer",
            homeAddress = "420 Oak Ave, San Francisco, CA"
        )
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveUserProfile(seedProfile) }.isSuccess)

        val tokenResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveFcmToken("fcm_device_token_987654")
        }
        assertTrue("Expected saveFcmToken update to succeed: ${tokenResult.exceptionOrNull()}", tokenResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile().getOrNull()
        }
        assertEquals("fcm_device_token_987654", fetched?.fcmToken)
        assertEquals("Sarah Mercer", fetched?.emergencyContactName)
        assertEquals("420 Oak Ave, San Francisco, CA", fetched?.homeAddress)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_profile@test.com"
        const val FCM_FRESH_EMAIL = "fcm_fresh@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
