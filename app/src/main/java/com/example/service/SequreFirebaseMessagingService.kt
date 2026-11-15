package com.example.service

import android.util.Log
import com.example.data.repository.SecurityRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles Firebase Cloud Messaging push notifications and token synchronization
 * for real-time updates when a booked security service starts or arrives at the user's location.
 */
class SequreFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCM_Service"
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed (${token.take(8)}...)")
        if (Firebase.auth.currentUser != null) {
            serviceScope.launch {
                try {
                    SecurityRepository(applicationContext).saveFcmToken(token)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist refreshed FCM token: ${e.message}")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val requestId = data["requestId"].orEmpty()
        val status = data["status"].orEmpty()
        val serviceName = data["serviceName"] ?: "Security Service"
        val guardName = data["guardName"]
        val locationName = data["locationName"]
        val etaMinutes = data["etaMinutes"]?.toLongOrNull()

        val copy = DispatchNotificationHelper.buildStatusNotificationCopy(
            status = status,
            serviceName = serviceName,
            guardName = guardName,
            locationName = locationName,
            etaMinutes = etaMinutes,
            explicitTitle = remoteMessage.notification?.title ?: data["title"],
            explicitBody = remoteMessage.notification?.body ?: data["body"]
        )

        DispatchNotificationHelper.showStatusUpdateNotification(
            context = this,
            requestId = requestId,
            status = status,
            title = copy.title,
            body = copy.body
        )

        DispatchNotificationHelper.emitForegroundStatusEvent(
            DispatchStatusEvent(
                requestId = requestId,
                status = status.ifBlank { "DISPATCHED" },
                title = copy.title,
                body = copy.body,
                guardName = guardName,
                locationName = locationName,
                isFromFcm = true
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
