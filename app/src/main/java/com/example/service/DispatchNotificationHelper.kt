package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DispatchStatusEvent(
    val requestId: String,
    val status: String,
    val title: String,
    val body: String,
    val guardName: String? = null,
    val locationName: String? = null,
    val isFromFcm: Boolean = false
)

data class StatusNotificationCopy(
    val title: String,
    val body: String
)

object DispatchNotificationHelper {

    const val DISPATCH_CHANNEL_ID = "sequre_dispatch_alerts"
    const val DISPATCH_CHANNEL_NAME = "Security Dispatch Alerts"

    const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
    const val EXTRA_STATUS = "EXTRA_STATUS"
    const val EXTRA_OPEN_TRACKING = "EXTRA_OPEN_TRACKING"

    private val _liveStatusEvents = MutableSharedFlow<DispatchStatusEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val liveStatusEvents: SharedFlow<DispatchStatusEvent> = _liveStatusEvents.asSharedFlow()

    fun emitForegroundStatusEvent(event: DispatchStatusEvent) {
        _liveStatusEvents.tryEmit(event)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureDispatchChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(DISPATCH_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    DISPATCH_CHANNEL_ID,
                    DISPATCH_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Real-time notifications when your booked security service starts or arrives at your location."
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 220L, 120L, 220L)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun buildStatusNotificationCopy(
        status: String,
        serviceName: String,
        guardName: String? = null,
        locationName: String? = null,
        etaMinutes: Long? = null,
        explicitTitle: String? = null,
        explicitBody: String? = null
    ): StatusNotificationCopy {
        val cleanService = serviceName.ifBlank { "Security Service" }
        val cleanGuard = guardName?.takeIf { it.isNotBlank() } ?: "Your assigned officer"
        val cleanLocation = locationName?.takeIf { it.isNotBlank() } ?: "your location"
        val normalizedStatus = status.trim().uppercase()

        val defaultCopy = when (normalizedStatus) {
            "STARTED", "DISPATCHED" -> StatusNotificationCopy(
                title = "Security Service Started • $cleanService",
                body = "$cleanGuard has started your $cleanService dispatch toward $cleanLocation" +
                    if (etaMinutes != null && etaMinutes > 0) " (ETA ~$etaMinutes min)." else "."
            )
            "EN_ROUTE" -> StatusNotificationCopy(
                title = "Guard En Route • $cleanService",
                body = "$cleanGuard is on the way to $cleanLocation" +
                    if (etaMinutes != null && etaMinutes > 0) " (ETA ~$etaMinutes min)." else "."
            )
            "ARRIVED", "ON_SITE" -> StatusNotificationCopy(
                title = "Guard Arrived at Location • $cleanService",
                body = "$cleanGuard has arrived at $cleanLocation and started on-site protection."
            )
            "COMPLETED" -> StatusNotificationCopy(
                title = "Security Service Completed • $cleanService",
                body = "Protection duty at $cleanLocation is complete. Tap to view summary."
            )
            "CANCELLED" -> StatusNotificationCopy(
                title = "Booking Cancelled • $cleanService",
                body = "Your $cleanService request for $cleanLocation was cancelled."
            )
            else -> StatusNotificationCopy(
                title = "Security Status Update • $cleanService",
                body = "Dispatch update for $cleanLocation."
            )
        }

        return StatusNotificationCopy(
            title = explicitTitle?.takeIf { it.isNotBlank() } ?: defaultCopy.title,
            body = explicitBody?.takeIf { it.isNotBlank() } ?: defaultCopy.body
        )
    }

    fun showStatusUpdateNotification(
        context: Context,
        requestId: String,
        status: String,
        title: String,
        body: String
    ): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        ensureDispatchChannel(notificationManager)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val normalizedStatus = status.trim().uppercase()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_STATUS, normalizedStatus)
            putExtra(EXTRA_OPEN_TRACKING, true)
        }

        val notificationId = if (requestId.isNotBlank()) {
            "status_${requestId}_$normalizedStatus".hashCode()
        } else {
            System.currentTimeMillis().toInt()
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, DISPATCH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0L, 220L, 120L, 220L))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            notificationManager.notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
