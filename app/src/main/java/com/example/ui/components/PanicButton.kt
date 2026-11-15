package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.PanicLocationProvider
import com.example.service.EmergencyNotificationHelper
import com.example.ui.PanicDispatchReceipt
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedDark
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import java.util.Locale
import kotlin.math.ceil

private const val SOS_HOLD_DURATION_MS = 1500

/**
 * Single high-visibility Hold-to-Activate SOS emergency button overlay.
 * Requires a continuous 1.5s press-and-hold to prevent accidental triggers;
 * releasing early cancels smoothly.
 */
@Composable
fun FloatingPanicButton(
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSosTransmitted: Boolean = false,
    onHoldStateChanged: (isHolding: Boolean, progress: Float) -> Unit = { _, _ -> }
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_panic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_panic_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_panic_halo"
    )

    val view = LocalView.current
    val context = LocalContext.current

    var isHolding by remember { mutableStateOf(false) }
    val holdProgress = remember { Animatable(0f) }

    LaunchedEffect(isHolding, isProcessing) {
        if (isHolding && !isProcessing) {
            var lastPulseStep = 0
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            EmergencyNotificationHelper.triggerHoldProgressTick(context, 0f)
            onHoldStateChanged(true, holdProgress.value)

            try {
                holdProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = SOS_HOLD_DURATION_MS, easing = LinearEasing)
                ) {
                    onHoldStateChanged(true, value)
                    val pulseProfile = EmergencyNotificationHelper.computeHoldPulseSchedule(value)
                    if (pulseProfile.stepIndex > lastPulseStep) {
                        lastPulseStep = pulseProfile.stepIndex
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        EmergencyNotificationHelper.triggerHoldProgressTick(context, value)
                    }
                }

                if (holdProgress.value >= 0.99f) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    EmergencyNotificationHelper.triggerPanicHaptics(context)
                    isHolding = false
                    onHoldStateChanged(false, 0f)
                    holdProgress.snapTo(0f)
                    onClick()
                }
            } finally {
                if (!isHolding) {
                    onHoldStateChanged(false, 0f)
                }
            }
        } else {
            onHoldStateChanged(false, 0f)
            if (holdProgress.value > 0f) {
                if (holdProgress.value < 0.99f) {
                    EmergencyNotificationHelper.triggerHoldCancelledHaptic(context)
                }
                holdProgress.animateTo(0f, tween(durationMillis = 160))
            }
        }
    }

    val currentProgress = holdProgress.value.coerceIn(0f, 1f)
    val remainingSec = ceil((SOS_HOLD_DURATION_MS * (1f - currentProgress)) / 100.0) / 10.0
    val scaleFactor = when {
        isProcessing -> 1f
        isHolding -> 1.04f + (currentProgress * 0.03f)
        else -> pulseScale
    }

    val iconTilt3D by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_3d_icon_tilt"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric animated crimson beacon halo
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(
                    if (isHolding) 1.22f + currentProgress * 0.16f
                    else 1f + (1f - haloAlpha) * 0.25f
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    EmergencyRed.copy(
                        alpha = if (isHolding) 0.38f else haloAlpha * 0.45f
                    )
                )
        )

        val pillShape = RoundedCornerShape(32.dp)

        // 3D Physical Extruded Bottom Shadow Layer
        Box(
            modifier = Modifier
                .heightIn(min = 58.dp)
                .scale(scaleFactor)
                .offset(y = if (isHolding) 1.5.dp else 4.5.dp)
                .clip(pillShape)
                .background(Color(0xFF7F1D1D)) // Deep obsidian-crimson 3D bevel base
        )

        Surface(
            shape = pillShape,
            color = if (isHolding) EmergencyRedDark else EmergencyRed,
            border = BorderStroke(
                width = if (isHolding) 2.dp else 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        EmergencyRed.copy(alpha = 0.5f)
                    )
                )
            ),
            modifier = Modifier
                .scale(scaleFactor)
                .graphicsLayer {
                    translationY = if (isHolding) 3.5.dp.toPx() else 0f
                    rotationX = if (isHolding) -3f else 0f
                    cameraDistance = 14f
                }
                .shadow(
                    elevation = if (isHolding) 2.dp else 8.dp,
                    shape = pillShape,
                    ambientColor = EmergencyRedDark.copy(alpha = 0.5f),
                    spotColor = EmergencyRed.copy(alpha = 0.8f)
                )
                .heightIn(min = 58.dp)
                .clip(pillShape)
                .pointerInput(isProcessing) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!isProcessing) {
                            isHolding = true
                            try {
                                waitForUpOrCancellation()
                            } finally {
                                isHolding = false
                            }
                        }
                    }
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Hold for 1.5 seconds to send SOS alert with GPS to emergency contacts and security dispatch"
                    onLongClick(label = "Activate SOS Emergency Alert") {
                        if (!isProcessing) {
                            EmergencyNotificationHelper.triggerPanicHaptics(context)
                            onClick()
                        }
                        true
                    }
                }
                .testTag("floating_panic_button")
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Top 3D specular highlight line
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(16.dp.toPx(), 2.dp.toPx()),
                        end = Offset(size.width - 16.dp.toPx(), 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            // Radial hold progress ring around 3D SOS icon
                            Canvas(modifier = Modifier.size(32.dp)) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.25f),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                if (currentProgress > 0f) {
                                    drawArc(
                                        color = Color.White,
                                        startAngle = -90f,
                                        sweepAngle = 360f * currentProgress,
                                        useCenter = false,
                                        style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .graphicsLayer {
                                        rotationZ = if (isHolding) 0f else iconTilt3D
                                        rotationY = if (isHolding) 18f else 0f
                                        cameraDistance = 12f
                                    }
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when {
                                    isProcessing -> "SENDING SOS"
                                    isHolding -> String.format(Locale.US, "SOS • %.1fs", remainingSec)
                                    else -> "SOS"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.3.sp,
                                fontSize = 16.sp
                            )
                            val dotBlinkAlpha by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 400, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "sos_button_dot_blink"
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSosTransmitted) {
                                            Color(0xFFFCA5A5).copy(alpha = dotBlinkAlpha)
                                        } else {
                                            Color(0xFF4ADE80)
                                        }
                                    )
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                        Text(
                            text = when {
                                isProcessing -> "Acquiring GPS"
                                isHolding -> "Release to cancel"
                                isSosTransmitted -> "Server notified"
                                else -> "Hold to activate"
                            },
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Persistent UI status indicator that starts solid green ("SOS READY")
 * and switches to blinking red ("SOS SENT") once the SOS alert has been
 * successfully transmitted to the Firestore server.
 * Styled with 3D tactile elevation and specular highlight.
 */
@Composable
fun SosTransmissionStatusIndicator(
    isTransmitted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blinkTransition = rememberInfiniteTransition(label = "sos_server_status_blink")
    val blinkFraction by blinkTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_status_blink_alpha"
    )

    val baseColor by animateColorAsState(
        targetValue = if (isTransmitted) EmergencyRed else TacticalGreen,
        animationSpec = tween(durationMillis = 250),
        label = "sos_status_base_color"
    )

    val dotAlpha = if (isTransmitted) blinkFraction else 1f
    val containerColor = if (isTransmitted) {
        EmergencyRed.copy(alpha = 0.14f + 0.18f * blinkFraction)
    } else {
        TacticalGreen.copy(alpha = 0.14f)
    }
    val borderColor = if (isTransmitted) {
        EmergencyRed.copy(alpha = 0.45f + 0.55f * blinkFraction)
    } else {
        TacticalGreen.copy(alpha = 0.7f)
    }

    val shape = RoundedCornerShape(20.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .heightIn(min = 36.dp)
            .shadow(
                elevation = 3.dp,
                shape = shape,
                ambientColor = baseColor.copy(alpha = 0.2f),
                spotColor = baseColor.copy(alpha = 0.35f)
            )
            .graphicsLayer {
                cameraDistance = 12f
            }
            .semantics(mergeDescendants = true) {
                contentDescription = if (isTransmitted) {
                    "SOS alert transmitted to server"
                } else {
                    "SOS system ready"
                }
            }
            .testTag("sos_transmission_status_indicator")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(baseColor.copy(alpha = dotAlpha))
                    .shadow(elevation = 2.dp, shape = CircleShape)
            )
            Text(
                text = if (isTransmitted) "SOS SENT" else "SOS READY",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = if (isTransmitted) EmergencyRedDark else TacticalGreen
            )
        }
    }
}

/**
 * High-visibility overlay HUD shown at top-center while the user holds the SOS button
 * or while GPS fix + emergency dispatch are in progress.
 */
@Composable
fun SosHoldProgressOverlay(
    visible: Boolean,
    isProcessing: Boolean,
    progress: Float,
    emergencyContactSummary: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + slideInVertically { -it / 2 },
        exit = fadeOut(tween(180)) + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        val clamped = progress.coerceIn(0f, 1f)
        val remainingSec = ceil((SOS_HOLD_DURATION_MS * (1f - clamped)) / 100.0) / 10.0

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EmergencyRedDark),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 16.dp)
                .testTag("sos_hold_overlay_hud")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(EmergencyRedDark, EmergencyRed)
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isProcessing) {
                                "TRANSMITTING LIVE GPS SOS..."
                            } else {
                                String.format(Locale.US, "KEEP HOLDING FOR SOS • %.1fs", remainingSec)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isProcessing) {
                                "Locking coordinates & alerting dispatch + emergency contact"
                            } else {
                                "Release finger anytime to cancel false alarm"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isProcessing) 1f else clamped },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (!emergencyContactSummary.isNullOrBlank()) {
                                "Emergency Contact: $emergencyContactSummary"
                            } else {
                                "No emergency contact saved — GPS share sheet ready on dispatch"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PanicConfirmationDialog(
    receipt: PanicDispatchReceipt,
    notificationSent: Boolean,
    smsComposerLaunched: Boolean = false,
    onSendEmergencySms: () -> Unit = {},
    onShareEmergencyLocation: () -> Unit = {},
    onConfigureEmergencyContact: () -> Unit = {},
    onViewTracking: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasPhone = !receipt.emergencyContactPhone.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmergencyRed.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SOS Alert Dispatched",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmergencyRedDark
                    )
                    Text(
                        text = if (notificationSent) "High-priority alarm notification sent" else "Logged to Command Center",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalGreen
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("panic_dispatched_dialog")
            ) {
                Text(
                    text = "Security dispatch received your emergency alert with your location:",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, SecurityCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (receipt.isLiveGps) TacticalGreen else SecurityBlueLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (receipt.isLiveGps) "Live GPS Coordinates" else "Fallback Site Location",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeutralTextPrimary
                            )
                        }
                        Text(
                            text = PanicLocationProvider.formatCoordinates(receipt.latitude, receipt.longitude),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmergencyRedDark
                        )
                        Text(
                            text = receipt.locationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary
                        )
                    }
                }

                // Emergency Contact Notification Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasPhone) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                    border = BorderStroke(
                        1.dp,
                        if (hasPhone) EmergencyRed.copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = null,
                                tint = if (hasPhone) EmergencyRed else Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasPhone) "Emergency Contact Alert" else "No Emergency Contact Phone",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NeutralTextPrimary
                                )
                                Text(
                                    text = receipt.emergencyContactSummary
                                        ?: "Add a trusted contact in Account for instant GPS SMS alerts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralTextSecondary
                                )
                            }
                        }

                        if (hasPhone) {
                            if (smsComposerLaunched) {
                                Text(
                                    text = "SMS composer opened with your live GPS map pin.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onSendEmergencySms,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmergencyRed,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("panic_send_sms_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (smsComposerLaunched) "Resend GPS SMS" else "Send GPS SMS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onShareEmergencyLocation,
                                    border = BorderStroke(1.dp, SecurityCardBorder),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("panic_share_gps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share GPS location",
                                        tint = NeutralTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onShareEmergencyLocation,
                                    border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("panic_share_gps_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = EmergencyRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share GPS",
                                        color = EmergencyRedDark,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                TextButton(
                                    onClick = onConfigureEmergencyContact,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("panic_configure_contact_button")
                                ) {
                                    Text(
                                        text = "Add Contact",
                                        color = SecurityBlueLight,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PurpleSurfaceTint,
                    border = BorderStroke(1.dp, SecurityBlueLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SecurityBlueLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = receipt.assignedUnit?.takeIf { it.isNotBlank() }
                                    ?: "Command Center Notified",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeutralTextPrimary
                            )
                            val subtitleText = when {
                                receipt.assignedUnit != null && receipt.etaMinutes != null ->
                                    "Priority Response • ETA ~${receipt.etaMinutes} mins"
                                receipt.assignedUnit != null ->
                                    "Priority Response Active"
                                else ->
                                    "Request in live queue • Awaiting unit assignment"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onViewTracking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("panic_view_map_button")
            ) {
                Text("Track Live Map", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("panic_dismiss_button")
            ) {
                Text("Dismiss", color = NeutralTextSecondary)
            }
        }
    )
}
