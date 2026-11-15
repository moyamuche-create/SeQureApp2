package com.example.ui.dispatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandCenterService
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen

@Composable
fun ShiftDurationCard(
    selectedService: CommandCenterService,
    durationHours: Long,
    onDurationChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shift Duration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SecurityBluePrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TacticalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rate: ${selectedService.priceLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TacticalGreen
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, SecurityCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (durationHours > 1) onDurationChanged(durationHours - 1) },
                    enabled = durationHours > 1,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (durationHours > 1) PurpleSurfaceTint else Color(0xFFF1F5F9))
                        .border(
                            1.dp,
                            if (durationHours > 1) SecurityBlueLight.copy(alpha = 0.5f) else SecurityCardBorder,
                            CircleShape
                        )
                        .testTag("decrement_hours_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease Hours",
                        tint = if (durationHours > 1) SecurityBlueLight else NeutralTextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$durationHours ${if (durationHours == 1L) "Hour" else "Hours"}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = NeutralTextPrimary
                    )
                    Text(
                        text = "Coverage duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralTextSecondary
                    )
                }

                IconButton(
                    onClick = { if (durationHours < 24) onDurationChanged(durationHours + 1) },
                    enabled = durationHours < 24,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (durationHours < 24) PurpleSurfaceTint else Color(0xFFF1F5F9))
                        .border(
                            1.dp,
                            if (durationHours < 24) SecurityBlueLight.copy(alpha = 0.5f) else SecurityCardBorder,
                            CircleShape
                        )
                        .testTag("increment_hours_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase Hours",
                        tint = if (durationHours < 24) SecurityBlueLight else NeutralTextMuted
                    )
                }
            }

            Text(
                text = "Quick presets",
                style = MaterialTheme.typography.labelMedium,
                color = NeutralTextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presets = listOf(
                    2L to "2 hrs",
                    4L to "4 hrs",
                    8L to "8 hrs",
                    12L to "12 hrs",
                    24L to "24 hrs"
                )

                items(presets) { (presetHours, label) ->
                    val isSelected = durationHours == presetHours
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDurationChanged(presetHours) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleSurfaceTint,
                            selectedLabelColor = SecurityBlueLight,
                            containerColor = Color.White,
                            labelColor = NeutralTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = NeutralBorder,
                            selectedBorderColor = SecurityBlueLight
                        ),
                        modifier = Modifier.testTag("preset_hours_$presetHours")
                    )
                }
            }
        }
    }
}

@Composable
fun BookingSummaryCard(
    tierTitle: String,
    service: CommandCenterService,
    startTimeLabel: String,
    durationHours: Long,
    location: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
            .testTag("booking_summary_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PurpleStripedBanner(
                height = 5.dp,
                stripeWidthDp = 8f,
                gapWidthDp = 8f
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = SecurityBlueLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Booking Summary",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SecurityBluePrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PurpleSurfaceTint
                    ) {
                        Text(
                            text = "${tierTitle.ifBlank { service.resolvedTierTitle }} Tier",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecurityBlueLight
                        )
                    }
                }

                SummaryRow(label = "Service", value = service.title)
                SummaryRow(label = "Start Time", value = startTimeLabel)
                SummaryRow(label = "Location", value = location.ifBlank { "Enter site address or sync GPS above" })

                HorizontalDivider(color = SecurityCardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Total ($durationHours ${if (durationHours == 1L) "hr" else "hrs"})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeutralTextPrimary
                    )
                    Text(
                        text = service.estimateTotalLabel(durationHours),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TacticalGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = NeutralTextSecondary,
            modifier = Modifier.width(84.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = NeutralTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
