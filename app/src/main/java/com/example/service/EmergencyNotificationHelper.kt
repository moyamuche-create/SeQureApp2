package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.location.PanicLocationProvider

data class HoldPulseProfile(
    val stepIndex: Int,
    val durationMs: Long,
    val amplitude: Int
)

object EmergencyNotificationHelper {

    const val PANIC_CHANNEL_ID = "sequre_panic_alerts"
    const val PANIC_CHANNEL_NAME = "High-Priority SOS Alerts"
    private const val PANIC_NOTIFICATION_ID = 91101

    /**
     * Maps hold progress (0f..1f) into 5 escalating physical pulse stages
     * so the user feels stronger, longer pulses as SOS activation approaches.
     */
    fun computeHoldPulseSchedule(progress: Float): HoldPulseProfile {
        val clamped = progress.coerceIn(0f, 1f)
        return when {
            clamped >= 0.80f -> HoldPulseProfile(stepIndex = 4, durationMs = 62L, amplitude = 255)
            clamped >= 0.60f -> HoldPulseProfile(stepIndex = 3, durationMs = 48L, amplitude = 215)
            clamped >= 0.40f -> HoldPulseProfile(stepIndex = 2, durationMs = 38L, amplitude = 175)
            clamped >= 0.20f -> HoldPulseProfile(stepIndex = 1, durationMs = 30L, amplitude = 135)
            else -> HoldPulseProfile(stepIndex = 0, durationMs = 24L, amplitude = 95)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerHoldProgressTick(context: Context, progress: Float = 0.5f) {
        try {
            val vibrator = resolveVibrator(context) ?: return
            val profile = computeHoldPulseSchedule(progress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = if (vibrator.hasAmplitudeControl()) {
                    profile.amplitude.coerceIn(1, 255)
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                val effect = VibrationEffect.createOneShot(profile.durationMs, amplitude)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attrs)
                } else {
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(profile.durationMs)
            }
        } catch (_: Exception) {
            // Safe no-op when vibrator hardware is unavailable
        }
    }

    fun triggerHoldCancelledHaptic(context: Context) {
        try {
            val vibrator = resolveVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vibrator.hasAmplitudeControl()) 70 else VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(18L, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18L)
            }
        } catch (_: Exception) {
            // Safe no-op
        }
    }

    fun triggerPanicHaptics(context: Context) {
        try {
            val vibrator = resolveVibrator(context) ?: return
            val pattern = longArrayOf(0L, 260L, 110L, 260L, 110L, 420L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(effect, attrs)
                } else {
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
            // Safe no-op on devices/emulators without vibrator hardware
        }
    }

    fun launchEmergencyContactSms(
        context: Context,
        phone: String,
        message: String
    ): Boolean {
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) return false
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(smsIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun launchEmergencyShareSheet(
        context: Context,
        message: String
    ): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SOS EMERGENCY ALERT")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "Notify Emergency Contact").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sendPanicDispatchNotification(
        context: Context,
        requestId: String,
        locationLabel: String,
        latitude: Double,
        longitude: Double,
        isLiveGps: Boolean,
        emergencyContactSummary: String? = null
    ): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        ensurePanicChannel(notificationManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                return false
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_REQUEST_ID", requestId)
            putExtra("EXTRA_STATUS", "REQUESTED")
            putExtra("EXTRA_IS_PANIC", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            PANIC_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasCoords = latitude != 0.0 || longitude != 0.0
        val coords = if (hasCoords) {
            PanicLocationProvider.formatCoordinates(latitude, longitude)
        } else {
            "GPS pending"
        }
        val sourceLabel = if (isLiveGps) "Live GPS Fix" else "Site Address"
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val expandedBody = buildString {
            append("High-priority SOS alert transmitted to Command Center.\n")
            append("Location ($sourceLabel): $locationLabel\n")
            append("Coordinates: $coords\n")
            append("Status: Awaiting Command Center officer assignment")
            if (!emergencyContactSummary.isNullOrBlank()) {
                append("\nEmergency Contact: $emergencyContactSummary")
            }
            if (requestId.isNotBlank()) {
                append("\nDispatch ID: $requestId")
            }
        }

        val notification = NotificationCompat.Builder(context, PANIC_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SOS ALERT SENT • COMMAND CENTER NOTIFIED")
            .setContentText("Location: $coords • Awaiting officer assignment")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0L, 350L, 150L, 350L))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            notificationManager.notify(PANIC_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun ensurePanicChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(PANIC_CHANNEL_ID)
            if (existing == null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val channel = NotificationChannel(
                    PANIC_CHANNEL_ID,
                    PANIC_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical emergency SOS notifications dispatched with live user GPS coordinates."
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 350L, 150L, 350L)
                    setSound(soundUri, audioAttributes)
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
