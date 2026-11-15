package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CommandCenterCatalog
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.CommandCenterService
import com.example.data.model.CommandCenterTier
import com.example.data.model.SecurityRequest
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SecurityRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing security services.")
    }

    /**
     * Observes active service tiers and services/pricing published by the standalone Command Center web app.
     */
    fun observeCommandCenterCatalog(): Flow<CommandCenterCatalog> = flow {
        val uid = requireUserId()
        val tiersFlow = db.collection(COLLECTION_TIERS)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommandCenterTier::class.java)
                    .sortedWith(compareBy<CommandCenterTier>({ it.sortOrder }, { it.title }))
            }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.LIST, COLLECTION_TIERS)
                throw error
            }

        val servicesFlow = db.collection(COLLECTION_SERVICES)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommandCenterService::class.java)
                    .sortedWith(compareBy<CommandCenterService>({ it.sortOrder }, { it.title }))
            }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.LIST, COLLECTION_SERVICES)
                throw error
            }

        emitAll(
            combine(tiersFlow, servicesFlow) { tiers, services ->
                CommandCenterCatalog(tiers = tiers, services = services)
            }
        )
    }

    /**
     * Observes live officer deployments assigned to the current user by the Command Center web app.
     */
    fun observeUserDeployments(): Flow<List<CommandCenterDeployment>> = flow {
        val uid = requireUserId()
        val query = db.collection(COLLECTION_DEPLOYMENTS)
            .whereEqualTo("clientUserId", uid)

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommandCenterDeployment::class.java)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_DEPLOYMENTS)
                    throw error
                }
        )
    }

    /**
     * Observes all security requests for the current authenticated user in real-time,
     * merging live Command Center updates from `/dispatches` and `/users/{uid}/security_requests`.
     */
    fun observeUserRequests(): Flow<List<SecurityRequest>> = flow {
        val uid = requireUserId()
        val dispatchesQuery = db.collection(COLLECTION_DISPATCHES)
            .whereEqualTo("userId", uid)
        val userMirrorQuery = db.collection("users")
            .document(uid)
            .collection("security_requests")

        val dispatchesFlow = dispatchesQuery.snapshots()
            .map { it.toObjects(SecurityRequest::class.java) }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.LIST, COLLECTION_DISPATCHES)
                throw error
            }

        val mirrorFlow = userMirrorQuery.snapshots()
            .map { it.toObjects(SecurityRequest::class.java) }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.LIST, "users/$uid/security_requests")
                throw error
            }

        emitAll(
            combine(dispatchesFlow, mirrorFlow) { dispatches, mirrored ->
                val merged = linkedMapOf<String, SecurityRequest>()
                mirrored.forEach { req ->
                    if (req.id.isNotBlank()) merged[req.id] = req
                }
                dispatches.forEach { dispatch ->
                    if (dispatch.id.isNotBlank()) {
                        val existing = merged[dispatch.id]
                        merged[dispatch.id] = if (existing == null) {
                            dispatch
                        } else {
                            pickFresherRequest(dispatch, existing)
                        }
                    }
                }
                merged.values.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        )
    }

    /**
     * Observes a specific security request for real-time tracking from Command Center dispatch updates.
     */
    fun observeRequest(requestId: String): Flow<SecurityRequest?> = flow {
        val uid = requireUserId()
        val dispatchRef = db.collection(COLLECTION_DISPATCHES).document(requestId)
        val mirrorRef = db.collection("users")
            .document(uid)
            .collection("security_requests")
            .document(requestId)

        val dispatchFlow = dispatchRef.snapshots()
            .map { it.toObject(SecurityRequest::class.java) }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.GET, "$COLLECTION_DISPATCHES/$requestId")
                throw error
            }

        val mirrorFlow = mirrorRef.snapshots()
            .map { it.toObject(SecurityRequest::class.java) }
            .catch { error ->
                if (error is CancellationException && auth.currentUser?.uid != uid) throw error
                handleFirestoreError(error, OperationType.GET, "users/$uid/security_requests/$requestId")
                throw error
            }

        emitAll(
            combine(dispatchFlow, mirrorFlow) { dispatchDoc, mirrorDoc ->
                when {
                    dispatchDoc != null && mirrorDoc != null -> pickFresherRequest(dispatchDoc, mirrorDoc)
                    dispatchDoc != null -> dispatchDoc
                    else -> mirrorDoc
                }
            }
        )
    }

    private fun pickFresherRequest(primary: SecurityRequest, secondary: SecurityRequest): SecurityRequest {
        val pSec = primary.updatedAt?.seconds ?: 0L
        val sSec = secondary.updatedAt?.seconds ?: 0L
        return when {
            pSec > sSec -> primary
            sSec > pSec -> secondary
            // Command Center writes directly to /dispatches, so prefer /dispatches on timestamp tie
            else -> primary
        }
    }

    /**
     * Submits a new on-demand, scheduled, or SOS dispatch request atomically to both
     * `/dispatches/{id}` (Command Center queue) and `/users/{uid}/security_requests/{id}`.
     * Always initializes with status = "REQUESTED" and zero client-assigned officer fields.
     */
    suspend fun createSecurityRequest(request: SecurityRequest): Result<String> {
        return try {
            val uid = requireUserId()
            val dispatchRef = db.collection(COLLECTION_DISPATCHES).document()
            val requestId = dispatchRef.id
            val userMirrorRef = db.collection("users")
                .document(uid)
                .collection("security_requests")
                .document(requestId)

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "serviceType" to request.serviceType,
                "serviceName" to request.serviceName,
                "status" to "REQUESTED",
                "isEmergency" to request.isEmergency,
                "locationName" to request.locationName,
                "latitude" to request.latitude,
                "longitude" to request.longitude,
                "targetDateTime" to request.targetDateTime,
                "isScheduled" to request.isScheduled,
                "durationHours" to request.durationHours,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            request.securityTier.takeIf { it.isNotBlank() }?.let { payload["securityTier"] = it }
            request.hourlyRate?.let { payload["hourlyRate"] = it }
            request.priceLabel?.takeIf { it.isNotBlank() }?.let { payload["priceLabel"] = it }
            request.estimatedTotalLabel?.takeIf { it.isNotBlank() }?.let { payload["estimatedTotalLabel"] = it }
            request.notes?.takeIf { it.isNotBlank() }?.let { payload["notes"] = it }

            val batch = db.batch()
            batch.set(dispatchRef, payload)
            batch.set(userMirrorRef, payload)
            batch.commit().await()

            Result.success(requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_DISPATCHES)
            Result.failure(e)
        }
    }

    /**
     * Submits rating and optional comment feedback for a completed mission across dispatch and user mirror docs.
     */
    suspend fun submitFeedback(requestId: String, rating: Int, comment: String?): Result<Unit> {
        return try {
            val uid = requireUserId()
            val dispatchRef = db.collection(COLLECTION_DISPATCHES).document(requestId)
            val mirrorRef = db.collection("users").document(uid).collection("security_requests").document(requestId)

            val updates = mutableMapOf<String, Any>(
                "rating" to rating,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            comment?.let {
                if (it.isNotBlank()) updates["feedbackComment"] = it.trim()
            }

            applyUpdatesToExistingDocs(dispatchRef, mirrorRef, updates)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_DISPATCHES/$requestId")
            Result.failure(e)
        }
    }

    /**
     * Cancels an active security request so the standalone Command Center sees the cancellation immediately.
     */
    suspend fun cancelRequest(requestId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val dispatchRef = db.collection(COLLECTION_DISPATCHES).document(requestId)
            val mirrorRef = db.collection("users").document(uid).collection("security_requests").document(requestId)

            val updates = mapOf<String, Any>(
                "status" to "CANCELLED",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            applyUpdatesToExistingDocs(dispatchRef, mirrorRef, updates)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_DISPATCHES/$requestId")
            Result.failure(e)
        }
    }

    private suspend fun applyUpdatesToExistingDocs(
        dispatchRef: com.google.firebase.firestore.DocumentReference,
        mirrorRef: com.google.firebase.firestore.DocumentReference,
        updates: Map<String, Any>
    ) {
        val dispatchExists = try {
            dispatchRef.get().await().exists()
        } catch (_: Exception) {
            false
        }
        val mirrorExists = try {
            mirrorRef.get().await().exists()
        } catch (_: Exception) {
            false
        }

        val batch = db.batch()
        var ops = 0
        if (dispatchExists) {
            batch.update(dispatchRef, updates)
            ops++
        }
        if (mirrorExists) {
            batch.update(mirrorRef, updates)
            ops++
        }
        if (ops == 0) {
            // Attempt direct update on dispatch queue so rule/not-found errors surface cleanly
            dispatchRef.update(updates).await()
        } else {
            batch.commit().await()
        }
    }

    /**
     * Gets user profile info.
     */
    suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val uid = requireUserId()
            val doc = db.collection("users").document(uid).get().await()
            Result.success(doc.toObject(UserProfile::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            Result.failure(e)
        }
    }

    /**
     * Creates or updates the user profile with structured emergency contact and saved home/business addresses.
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val doc = docRef.get().await()

            val cleanName = profile.displayName.trim().ifBlank { "Verified Client" }
            val cleanEmail = profile.email.trim().ifBlank { auth.currentUser?.email ?: "client@sequre.app" }
            val cleanPhone = profile.phoneNumber?.trim()?.takeIf { it.isNotBlank() }
            val cleanEmergencyName = profile.emergencyContactName?.trim()?.takeIf { it.isNotBlank() }
            val cleanEmergencyPhone = profile.emergencyContactPhone?.trim()?.takeIf { it.isNotBlank() }
            val cleanEmergencyRelation = profile.emergencyContactRelation?.trim()?.takeIf { it.isNotBlank() }
            val cleanEmergencySummary = UserProfile.formatEmergencyContactSummary(
                name = cleanEmergencyName,
                phone = cleanEmergencyPhone,
                relation = cleanEmergencyRelation,
                legacySummary = profile.emergencyContact
            )
            val cleanHome = profile.homeAddress?.trim()?.takeIf { it.isNotBlank() }
            val cleanBusiness = profile.businessAddress?.trim()?.takeIf { it.isNotBlank() }
            val cleanPreferredType = profile.preferredAddressType?.uppercase()?.takeIf {
                it == UserProfile.ADDRESS_TYPE_HOME || it == UserProfile.ADDRESS_TYPE_BUSINESS
            }
            val cleanDefaultLocation = profile.copy(
                homeAddress = cleanHome,
                businessAddress = cleanBusiness,
                preferredAddressType = cleanPreferredType
            ).resolvedDefaultAddress

            if (!doc.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "email" to cleanEmail,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                cleanPhone?.let { payload["phoneNumber"] = it }
                cleanEmergencySummary?.let { payload["emergencyContact"] = it }
                cleanEmergencyName?.let { payload["emergencyContactName"] = it }
                cleanEmergencyPhone?.let { payload["emergencyContactPhone"] = it }
                cleanEmergencyRelation?.let { payload["emergencyContactRelation"] = it }
                cleanHome?.let { payload["homeAddress"] = it }
                cleanBusiness?.let { payload["businessAddress"] = it }
                cleanPreferredType?.let { payload["preferredAddressType"] = it }
                cleanDefaultLocation?.let { payload["defaultLocation"] = it }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "phoneNumber" to (cleanPhone ?: FieldValue.delete()),
                    "emergencyContact" to (cleanEmergencySummary ?: FieldValue.delete()),
                    "emergencyContactName" to (cleanEmergencyName ?: FieldValue.delete()),
                    "emergencyContactPhone" to (cleanEmergencyPhone ?: FieldValue.delete()),
                    "emergencyContactRelation" to (cleanEmergencyRelation ?: FieldValue.delete()),
                    "homeAddress" to (cleanHome ?: FieldValue.delete()),
                    "businessAddress" to (cleanBusiness ?: FieldValue.delete()),
                    "preferredAddressType" to (cleanPreferredType ?: FieldValue.delete()),
                    "defaultLocation" to (cleanDefaultLocation ?: FieldValue.delete())
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    /**
     * Registers or refreshes the user's Firebase Cloud Messaging token in Firestore
     * so push notifications can target this device when dispatch status changes.
     */
    suspend fun saveFcmToken(token: String): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return Result.success(Unit)
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                val user = auth.currentUser
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to (user?.displayName?.takeIf { it.isNotBlank() } ?: "Verified Client"),
                    "email" to (user?.email?.takeIf { it.isNotBlank() } ?: "client@sequre.app"),
                    "fcmToken" to cleanToken,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "fcmToken" to cleanToken,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_TIERS = "command_center_tiers"
        const val COLLECTION_SERVICES = "command_center_services"
        const val COLLECTION_DISPATCHES = "dispatches"
        const val COLLECTION_DEPLOYMENTS = "deployments"
    }
}
