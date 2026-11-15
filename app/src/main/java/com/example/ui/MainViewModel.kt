package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.location.GpsCoordinateFix
import com.example.data.location.PanicLocationProvider
import com.example.data.model.CommandCenterCatalog
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.SecurityRequest
import com.example.data.model.UserProfile
import com.example.data.repository.SecurityRepository
import com.example.service.DispatchNotificationHelper
import com.example.service.DispatchStatusEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class PanicDispatchReceipt(
    val requestId: String,
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val isLiveGps: Boolean,
    val accuracyMeters: Float?,
    val assignedUnit: String? = null,
    val etaMinutes: Long? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactSummary: String? = null,
    val emergencyAlertMessage: String = ""
)

class MainViewModel(
    private val repository: SecurityRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _selectedRequest = MutableStateFlow<SecurityRequest?>(null)

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _lastPanicReceipt = MutableStateFlow<PanicDispatchReceipt?>(null)
    val lastPanicReceipt: StateFlow<PanicDispatchReceipt?> = _lastPanicReceipt.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isSavingProfile = MutableStateFlow(false)
    val isSavingProfile: StateFlow<Boolean> = _isSavingProfile.asStateFlow()

    private val _fcmRegistrationActive = MutableStateFlow(false)
    val fcmRegistrationActive: StateFlow<Boolean> = _fcmRegistrationActive.asStateFlow()

    private val _realtimeStatusAlert = MutableStateFlow<DispatchStatusEvent?>(null)
    val realtimeStatusAlert: StateFlow<DispatchStatusEvent?> = _realtimeStatusAlert.asStateFlow()

    private val _isSosTransmittedToServer = MutableStateFlow(false)
    val isSosTransmittedToServer: StateFlow<Boolean> = _isSosTransmittedToServer.asStateFlow()

    private val lastSeenStatuses = mutableMapOf<String, String>()
    private var hasHydratedInitialRequests = false

    private val _refreshTrigger = MutableStateFlow(0)
    private val _catalogRefreshTrigger = MutableStateFlow(0)

    // Real-time Command Center catalog (tiers, services, dynamic prices)
    @OptIn(ExperimentalCoroutinesApi::class)
    val catalogState: StateFlow<UiState<CommandCenterCatalog>> = _catalogRefreshTrigger
        .flatMapLatest {
            repository.observeCommandCenterCatalog()
                .map<CommandCenterCatalog, UiState<CommandCenterCatalog>> { catalog ->
                    UiState.Success(catalog)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    emit(UiState.Error(rootError.message ?: "Failed to sync services from Command Center"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // Real-time Command Center officer deployments for the authenticated user
    @OptIn(ExperimentalCoroutinesApi::class)
    val deploymentsState: StateFlow<List<CommandCenterDeployment>> = _refreshTrigger
        .flatMapLatest {
            repository.observeUserDeployments()
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(emptyList())
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Real-time observation of user bookings & dispatches from Command Center
    @OptIn(ExperimentalCoroutinesApi::class)
    val requestsState: StateFlow<UiState<List<SecurityRequest>>> = _refreshTrigger
        .flatMapLatest {
            repository.observeUserRequests()
                .map<List<SecurityRequest>, UiState<List<SecurityRequest>>> { requests ->
                    detectStatusTransitions(requests)
                    _isSosTransmittedToServer.value = requests.any { it.isEmergency && it.isActive }
                    UiState.Success(requests)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    emit(UiState.Error(rootError.message ?: "Failed to load dispatches from Command Center"))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        loadUserProfile()
    }

    private fun detectStatusTransitions(requests: List<SecurityRequest>) {
        if (!hasHydratedInitialRequests) {
            requests.forEach { req ->
                if (req.id.isNotBlank()) {
                    lastSeenStatuses[req.id] = req.status
                }
            }
            hasHydratedInitialRequests = true
            return
        }

        for (req in requests) {
            if (req.id.isBlank()) continue
            val previousStatus = lastSeenStatuses[req.id]
            lastSeenStatuses[req.id] = req.status

            if (previousStatus != null && previousStatus != req.status && !req.isEmergency) {
                if (req.status in setOf("DISPATCHED", "EN_ROUTE", "ON_SITE", "COMPLETED")) {
                    val copy = DispatchNotificationHelper.buildStatusNotificationCopy(
                        status = req.status,
                        serviceName = req.serviceName,
                        guardName = req.guardName,
                        locationName = req.locationName,
                        etaMinutes = req.etaMinutes
                    )
                    _realtimeStatusAlert.value = DispatchStatusEvent(
                        requestId = req.id,
                        status = req.status,
                        title = copy.title,
                        body = copy.body,
                        guardName = req.guardName,
                        locationName = req.locationName,
                        isFromFcm = false
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun registerFcmTokenAndTopics() {
        viewModelScope.launch {
            try {
                val messaging = FirebaseMessaging.getInstance()
                messaging.subscribeToTopic("user_$currentUserId")
                messaging.subscribeToTopic("dispatch_updates_$currentUserId")
                val token = messaging.token.await()
                if (!token.isNullOrBlank()) {
                    val saveRes = repository.saveFcmToken(token)
                    _fcmRegistrationActive.value = saveRes.isSuccess
                }
            } catch (_: Exception) {
                // Safe no-op in headless/Play-Services-absent test environments
            }
        }
    }

    fun dismissRealtimeStatusAlert() {
        _realtimeStatusAlert.value = null
    }

    fun selectRequestById(requestId: String) {
        val currentRequests = (requestsState.value as? UiState.Success)?.data.orEmpty()
        val match = currentRequests.firstOrNull { it.id == requestId }
        if (match != null) {
            _selectedRequest.value = match
        }
    }

    fun retryLoadBookings() {
        _refreshTrigger.value += 1
    }

    fun retryLoadCatalog() {
        _catalogRefreshTrigger.value += 1
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val result = repository.getUserProfile()
            if (result.isSuccess) {
                _userProfile.value = result.getOrNull()
            }
        }
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun dismissPanicReceipt() {
        _lastPanicReceipt.value = null
    }

    fun selectActiveRequest(request: SecurityRequest) {
        _selectedRequest.value = request
    }

    /**
     * Resolve the active request to show in Tracking / Map tabs:
     * either explicitly selected or the newest active request from Command Center.
     */
    fun resolveActiveRequest(requests: List<SecurityRequest>): SecurityRequest? {
        val selected = _selectedRequest.value
        if (selected != null) {
            val updated = requests.firstOrNull { it.id == selected.id }
            if (updated != null) {
                _selectedRequest.value = updated
                return updated
            }
            return selected
        }
        return requests.firstOrNull { it.isActive } ?: requests.firstOrNull()
    }

    /**
     * Matches the live Command Center deployment record for a dispatch request if assigned.
     */
    fun resolveDeploymentForRequest(
        request: SecurityRequest?,
        deployments: List<CommandCenterDeployment>
    ): CommandCenterDeployment? {
        if (request == null) return null
        return deployments.firstOrNull { dep ->
            (request.deploymentId != null && dep.id == request.deploymentId) ||
                dep.requestId == request.id
        }
    }

    /**
     * Submit on-demand or scheduled request to the standalone Command Center queue.
     */
    fun submitSecurityRequest(request: SecurityRequest, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createSecurityRequest(request)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "${request.serviceName} sent to Command Center."
                onComplete()
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to send request to Command Center"
            }
        }
    }

    /**
     * Trigger High-Priority SOS Alert with the user's current GPS coordinates.
     * Submitted to Command Center with status REQUESTED; unit assignment comes from Command Center.
     */
    fun triggerPanicDispatch(
        gpsFix: GpsCoordinateFix,
        onDispatched: (PanicDispatchReceipt) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val profile = _userProfile.value
            val emergencyContact = profile?.effectiveEmergencyContactSummary
            val emergencyContactName = profile?.emergencyContactName?.takeIf { it.isNotBlank() }
            val emergencyContactPhone = profile?.emergencyContactPhone?.takeIf { it.isNotBlank() }
            val senderName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: Firebase.auth.currentUser?.displayName
            val dispatchNotes = PanicLocationProvider.buildPanicDispatchNotes(
                latitude = gpsFix.latitude,
                longitude = gpsFix.longitude,
                accuracyMeters = gpsFix.accuracyMeters,
                isLiveGps = gpsFix.isLiveGps,
                emergencyContact = emergencyContact
            )
            val emergencyMessage = PanicLocationProvider.buildEmergencyContactAlertMessage(
                senderName = senderName,
                emergencyContactName = emergencyContactName,
                latitude = gpsFix.latitude,
                longitude = gpsFix.longitude,
                locationLabel = gpsFix.addressLabel,
                isLiveGps = gpsFix.isLiveGps,
                accuracyMeters = gpsFix.accuracyMeters
            )

            val emergencyService = (catalogState.value as? UiState.Success)?.data?.findEmergencyService()

            val emergencyRequest = SecurityRequest(
                securityTier = emergencyService?.resolvedTierTitle ?: "Emergency",
                serviceType = emergencyService?.code ?: "SOS_EMERGENCY",
                serviceName = emergencyService?.title ?: "High-Priority SOS Alert",
                status = "REQUESTED",
                isEmergency = true,
                locationName = gpsFix.addressLabel,
                latitude = gpsFix.latitude,
                longitude = gpsFix.longitude,
                targetDateTime = "Immediate (SOS)",
                isScheduled = false,
                durationHours = 1L,
                hourlyRate = emergencyService?.hourlyRate,
                priceLabel = emergencyService?.priceLabel,
                notes = dispatchNotes
            )

            val result = repository.createSecurityRequest(emergencyRequest)
            _isSubmitting.value = false
            if (result.isSuccess) {
                val requestId = result.getOrThrow()
                _isSosTransmittedToServer.value = true
                _selectedRequest.value = emergencyRequest.copy(id = requestId)
                val coordLabel = if (gpsFix.latitude != 0.0 || gpsFix.longitude != 0.0) {
                    PanicLocationProvider.formatCoordinates(gpsFix.latitude, gpsFix.longitude)
                } else {
                    gpsFix.addressLabel
                }
                val receipt = PanicDispatchReceipt(
                    requestId = requestId,
                    locationLabel = gpsFix.addressLabel,
                    latitude = gpsFix.latitude,
                    longitude = gpsFix.longitude,
                    isLiveGps = gpsFix.isLiveGps,
                    accuracyMeters = gpsFix.accuracyMeters,
                    assignedUnit = null,
                    etaMinutes = null,
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone,
                    emergencyContactSummary = emergencyContact,
                    emergencyAlertMessage = emergencyMessage
                )
                _lastPanicReceipt.value = receipt
                _feedbackMessage.value = "SOS SENT TO COMMAND CENTER ($coordLabel)"
                onDispatched(receipt)
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "SOS transmission failed"
            }
        }
    }

    /**
     * Cancel an active request.
     */
    fun cancelSecurityRequest(request: SecurityRequest) {
        viewModelScope.launch {
            val result = repository.cancelRequest(request.id)
            if (result.isSuccess) {
                if (request.isEmergency) {
                    _isSosTransmittedToServer.value = false
                }
                if (_selectedRequest.value?.id == request.id) {
                    _selectedRequest.value = request.copy(status = "CANCELLED")
                }
                _feedbackMessage.value = "Security request cancelled."
            }
        }
    }

    /**
     * Submit feedback and rating for completed security mission.
     */
    fun submitFeedback(requestId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            val result = repository.submitFeedback(requestId, rating, comment)
            if (result.isSuccess) {
                _feedbackMessage.value = "Thank you! Rating submitted successfully."
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit feedback"
            }
        }
    }

    /**
     * Save structured user profile details including emergency contact info and saved home/business addresses.
     */
    fun saveProfile(
        name: String,
        phone: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        emergencyContactRelation: String,
        homeAddress: String,
        businessAddress: String,
        preferredAddressType: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSavingProfile.value = true
            val user = Firebase.auth.currentUser
            val email = user?.email ?: _userProfile.value?.email ?: "client@sequre.app"
            val trimmedName = name.trim().ifBlank { user?.displayName ?: "Verified Client" }
            val cleanPhone = phone.trim().ifBlank { null }
            val cleanEmergencyName = emergencyContactName.trim().ifBlank { null }
            val cleanEmergencyPhone = emergencyContactPhone.trim().ifBlank { null }
            val cleanEmergencyRelation = emergencyContactRelation.trim().ifBlank { null }
            val cleanSummary = UserProfile.formatEmergencyContactSummary(
                name = cleanEmergencyName,
                phone = cleanEmergencyPhone,
                relation = cleanEmergencyRelation
            )
            val cleanHome = homeAddress.trim().ifBlank { null }
            val cleanBusiness = businessAddress.trim().ifBlank { null }
            val cleanPreferred = preferredAddressType.uppercase().takeIf {
                it == UserProfile.ADDRESS_TYPE_HOME || it == UserProfile.ADDRESS_TYPE_BUSINESS
            } ?: UserProfile.ADDRESS_TYPE_HOME

            val draftProfile = UserProfile(
                id = currentUserId,
                userId = currentUserId,
                displayName = trimmedName,
                email = email,
                phoneNumber = cleanPhone,
                emergencyContact = cleanSummary,
                emergencyContactName = cleanEmergencyName,
                emergencyContactPhone = cleanEmergencyPhone,
                emergencyContactRelation = cleanEmergencyRelation,
                homeAddress = cleanHome,
                businessAddress = cleanBusiness,
                preferredAddressType = cleanPreferred,
                createdAt = _userProfile.value?.createdAt
            )
            val resolvedProfile = draftProfile.copy(defaultLocation = draftProfile.resolvedDefaultAddress)

            val result = repository.saveUserProfile(resolvedProfile)
            _isSavingProfile.value = false
            if (result.isSuccess) {
                _userProfile.value = resolvedProfile
                _feedbackMessage.value = "Profile, emergency contact & addresses saved."
                onComplete(true)
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
                onComplete(false)
            }
        }
    }

    class Factory(
        private val repository: SecurityRepository,
        private val currentUserId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, currentUserId) as T
        }
    }
}
