package com.example.ui.tracking

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityRequest
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen

@Composable
fun ServiceStatusTrackerComponent(
    request: SecurityRequest?,
    modifier: Modifier = Modifier
) {
    if (request == null) return

    val status = request.status.uppercase()
    val displayStatus = when (status) {
        "REQUESTED" -> "Pending"
        "DISPATCHED", "EN_ROUTE" -> "Dispatched"
        "ON_SITE" -> "On-Site"
        "COMPLETED" -> "Completed"
        "CANCELLED" -> "Cancelled"
        else -> "Pending"
    }

    val statusColor = when (displayStatus) {
        "Pending" -> GoldLight
        "Dispatched" -> SecurityBlueLight
        "On-Site" -> TacticalGreen
        "Completed" -> TacticalGreen
        else -> NeutralTextMuted
    }

    val statusIcon = when (displayStatus) {
        "Pending" -> Icons.Default.HourglassTop
        "Dispatched" -> Icons.Default.LocalShipping
        "On-Site" -> Icons.Default.Shield
        "Completed" -> Icons.Default.CheckCircle
        else -> Icons.Default.HourglassTop
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_status_tracker_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = displayStatus,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Service Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeutralTextSecondary
                        )
                        Text(
                            text = request.serviceName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralTextPrimary
                        )
                    }
                }

                // Status Badge with Pulse Effect
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                    modifier = Modifier.alpha(if (displayStatus != "Completed") alphaAnim else 1f)
                ) {
                    Text(
                        text = displayStatus,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            // Real-time Firestore sync indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, SecurityCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TacticalGreen)
                    )
                    Text(
                        text = "Real-time sync active",
                        fontSize = 12.sp,
                        color = NeutralTextSecondary
                    )
                }

                Text(
                    text = "ID: ${request.id.take(8)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecurityBluePrimary
                )
            }
        }
    }
}
