package com.example.ui.dispatch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.TacticalGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarTrackingCanvas(
    modifier: Modifier = Modifier,
    status: String,
    isEmergency: Boolean,
    etaMinutes: Long? = null,
    guardName: String? = null,
    guardBadge: String? = null,
    guardDistanceFactor: Float? = null,
    guardBearingDegrees: Double? = null,
    rangeMeters: Double = 1000.0,
    distanceLabel: String? = null,
    bearingLabel: String? = null,
    coordinatesLabel: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    val textMeasurer = rememberTextMeasurer()

    // Continuous 360 degree radar sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    // Pulse wave for user beacon
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeaconPulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val targetFactor = guardDistanceFactor ?: when (status) {
        "REQUESTED" -> 0.85f
        "DISPATCHED" -> 0.70f
        "EN_ROUTE" -> 0.42f
        "ON_SITE" -> 0.10f
        else -> 0.0f
    }
    val animatedDistanceFactor by animateFloatAsState(
        targetValue = targetFactor.coerceIn(0f, 0.94f),
        animationSpec = tween(durationMillis = 650),
        label = "GuardRadarFactor"
    )

    val bearingDeg = guardBearingDegrees ?: 315.0
    val isActiveMission = status != "STANDBY" && status != "COMPLETED" && status != "CANCELLED"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E091A))
            .border(
                width = 1.2.dp,
                color = if (isEmergency) EmergencyRed.copy(alpha = 0.65f) else Color(0xFF3B2168),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.47f)
            val maxRadius = minOf(size.width, size.height * 0.86f) * 0.43f

            // Radar background gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1033), Color(0xFF080512)),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            val gridColor = Color(0xFF3B2168)
            val accentColor = if (isEmergency) EmergencyRed else TacticalGreen

            // Concentric range circles
            val ringCount = 4
            for (i in 1..ringCount) {
                val r = (maxRadius / ringCount) * i
                drawCircle(
                    color = gridColor.copy(alpha = if (i == ringCount) 0.95f else 0.7f),
                    radius = r,
                    center = center,
                    style = Stroke(width = if (i == ringCount) 1.6f else 1.1f)
                )
            }

            // Crosshair lines (N-S, E-W)
            drawLine(
                color = gridColor,
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = gridColor,
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1f
            )

            // 45 degree diagonal guides
            val diag = maxRadius * 0.7071f
            drawLine(
                color = gridColor.copy(alpha = 0.45f),
                start = Offset(center.x - diag, center.y - diag),
                end = Offset(center.x + diag, center.y + diag),
                strokeWidth = 0.9f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
            drawLine(
                color = gridColor.copy(alpha = 0.45f),
                start = Offset(center.x - diag, center.y + diag),
                end = Offset(center.x + diag, center.y - diag),
                strokeWidth = 0.9f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Cardinal Compass Markers (N / E / S / W)
            val compassStyle = TextStyle(
                color = Color(0xFFC4B5FD),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            val northLayout = textMeasurer.measure("N", compassStyle)
            drawText(
                textLayoutResult = northLayout,
                topLeft = Offset(
                    center.x - northLayout.size.width / 2f,
                    center.y - maxRadius - northLayout.size.height - 4f
                )
            )
            val eastLayout = textMeasurer.measure("E", compassStyle)
            drawText(
                textLayoutResult = eastLayout,
                topLeft = Offset(
                    center.x + maxRadius + 6f,
                    center.y - eastLayout.size.height / 2f
                )
            )
            val westLayout = textMeasurer.measure("W", compassStyle)
            drawText(
                textLayoutResult = westLayout,
                topLeft = Offset(
                    center.x - maxRadius - westLayout.size.width - 6f,
                    center.y - westLayout.size.height / 2f
                )
            )

            // Sweep Wedge Trail (48-degree trailing sector)
            val sweepWedgeAngle = 48f
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.75f to accentColor.copy(alpha = 0.08f),
                    1.0f to accentColor.copy(alpha = 0.32f),
                    center = center
                ),
                startAngle = sweepAngle - sweepWedgeAngle,
                sweepAngle = sweepWedgeAngle,
                useCenter = true,
                topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                size = Size(maxRadius * 2f, maxRadius * 2f)
            )

            // Leading sweep beam line
            val rad = Math.toRadians(sweepAngle.toDouble())
            val sweepTarget = Offset(
                x = center.x + (maxRadius * cos(rad)).toFloat(),
                y = center.y + (maxRadius * sin(rad)).toFloat()
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), accentColor),
                    start = center,
                    end = sweepTarget
                ),
                start = center,
                end = sweepTarget,
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )

            // Guard unit position (0 degrees = North / top -> angleRad = bearing - 90)
            val guardAngleRad = Math.toRadians(bearingDeg - 90.0)
            val guardPos = Offset(
                x = center.x + (maxRadius * animatedDistanceFactor * cos(guardAngleRad)).toFloat(),
                y = center.y + (maxRadius * animatedDistanceFactor * sin(guardAngleRad)).toFloat()
            )

            if (isActiveMission) {
                // Vector line between Guard and User
                drawLine(
                    color = GoldAccent.copy(alpha = 0.78f),
                    start = guardPos,
                    end = center,
                    strokeWidth = 2.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                // Guard unit pulse halo & pin
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.22f + pulseAlpha * 0.25f),
                    radius = 18f + (1f - pulseAlpha) * 6f,
                    center = guardPos
                )
                drawCircle(
                    color = GoldAccent,
                    radius = 9f,
                    center = guardPos
                )
                drawCircle(
                    color = Color(0xFF0E091A),
                    radius = 3.5f,
                    center = guardPos
                )
            }

            // User target beacon (center)
            drawCircle(
                color = (if (isEmergency) EmergencyRed else SecurityBlueLight).copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = center
            )
            drawCircle(
                color = if (isEmergency) EmergencyRed else SecurityBlueLight,
                radius = 8f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 3.2f,
                center = center
            )
        }

        // Top-Left: Live Tracking / Emergency Status Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE6140D26))
                .border(
                    1.dp,
                    if (isEmergency) EmergencyRed.copy(alpha = 0.7f) else Color(0xFF5B21B6),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isEmergency) EmergencyRed else TacticalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEmergency) "SOS RADAR LOCK" else "LIVE RADAR",
                    color = if (isEmergency) Color(0xFFFCA5A5) else Color(0xFF34D399),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top-Right: Range & ETA Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xE6140D26))
                .border(1.dp, Color(0xFF5B21B6), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            val rangeLabel = if (rangeMeters >= 1000.0) "${(rangeMeters / 1000.0).toInt()}km" else "${rangeMeters.toInt()}m"
            val etaText = when (status) {
                "STANDBY" -> "Standby • $rangeLabel"
                "REQUESTED" -> "Awaiting Command • $rangeLabel"
                "DISPATCHED" -> if (etaMinutes != null) "ETA ~${etaMinutes}m • $rangeLabel" else "Assigned • $rangeLabel"
                "EN_ROUTE" -> if (etaMinutes != null) "ETA ${etaMinutes}m • $rangeLabel" else "En Route • $rangeLabel"
                "ON_SITE" -> "On Site • $rangeLabel"
                "COMPLETED" -> "Mission Complete"
                "CANCELLED" -> "Cancelled"
                else -> "Range $rangeLabel"
            }
            Text(
                text = etaText,
                color = Color(0xFFFCD34D),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Telemetry Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xD9090614))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        !guardName.isNullOrBlank() -> "$guardName (${guardBadge ?: "Unit"})"
                        status == "STANDBY" -> "Perimeter Standby • No Active Unit"
                        else -> "Dispatching nearest responder..."
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!distanceLabel.isNullOrBlank() || !bearingLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = listOfNotNull(distanceLabel, bearingLabel).joinToString(" • "),
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!coordinatesLabel.isNullOrBlank()) {
                Text(
                    text = "Target GPS: $coordinatesLabel",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
