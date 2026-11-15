package com.example.ui.dispatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CommandCenterService
import com.example.data.model.CommandCenterTier
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.PurpleStripedRecommendedBadge
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleBorderTint
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen

@Composable
fun SecurityTierSelector(
    tiers: List<CommandCenterTier>,
    totalServiceCount: Int,
    selectedTierCode: String?,
    onTierSelected: (CommandCenterTier?) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSelectedTier = tiers.firstOrNull { it.code.equals(selectedTierCode, ignoreCase = true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "1. Select Security Tier",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SecurityBluePrimary
                )
                Text(
                    text = "Live coverage tiers from Command Center",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )
            }

            val isAllSelected = selectedTierCode == null
            Surface(
                onClick = { onTierSelected(null) },
                shape = RoundedCornerShape(24.dp),
                color = if (isAllSelected) SecurityBlueLight else PurpleSurfaceTint,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isAllSelected) SecurityBlueLight else PurpleBorderTint
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("tier_chip_all")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "All ($totalServiceCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAllSelected) Color.White else SecurityBlueLight
                    )
                }
            }
        }

        if (tiers.isNotEmpty()) {
            tiers.chunked(3).forEach { rowTiers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiers.forEach { tier ->
                        val isSelected = selectedTierCode?.equals(tier.code, ignoreCase = true) == true
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1.0f,
                            animationSpec = tween(durationMillis = 220),
                            label = "tier_scale"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> PurpleSurfaceTint
                                tier.isRecommended -> Color(0xFFFAF8FF)
                                else -> Color.White
                            },
                            animationSpec = tween(durationMillis = 220),
                            label = "tier_bg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> SecurityBlueLight
                                tier.isRecommended -> PurpleBorderTint
                                else -> SecurityCardBorder
                            },
                            animationSpec = tween(durationMillis = 220),
                            label = "tier_border"
                        )
                        val borderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.dp else if (tier.isRecommended) 1.4.dp else 1.dp,
                            animationSpec = tween(durationMillis = 220),
                            label = "tier_border_width"
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 124.dp)
                                .scale(animatedScale)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTierSelected(tier) }
                                .testTag("tier_card_${tier.code.lowercase()}"),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(width = borderWidth, color = borderColor),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 3.dp else 1.dp
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (tier.isRecommended) {
                                    PurpleStripedBanner(
                                        height = 4.dp,
                                        stripeWidthDp = 6f,
                                        gapWidthDp = 6f
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(PurpleSurfaceTint)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) SecurityBlueLight else PurpleBorderTint,
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(
                                                    id = when {
                                                        tier.code.contains("PERSONAL", ignoreCase = true) ||
                                                            tier.code.contains("VIP", ignoreCase = true) ->
                                                            R.drawable.img_tier_personal_1794545788139
                                                        tier.code.contains("EVENT", ignoreCase = true) ->
                                                            R.drawable.img_tier_event_1794545788503
                                                        else -> R.drawable.img_tier_property_1794545788126
                                                    }
                                                ),
                                                contentDescription = "${tier.title} tier icon",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected tier",
                                                tint = SecurityBlueLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (tier.isRecommended) {
                                        PurpleStripedRecommendedBadge(
                                            text = "TOP PICK",
                                            modifier = Modifier.testTag("recommended_tier_badge")
                                        )
                                    }

                                    Text(
                                        text = tier.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = NeutralTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (tier.subtitle.isNotBlank()) {
                                        Text(
                                            text = tier.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralTextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (tier.startingRate.isNotBlank()) {
                                        Text(
                                            text = tier.startingRate,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TacticalGreen,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (rowTiers.size < 3 && tiers.size > 3) {
                        repeat(3 - rowTiers.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = activeSelectedTier != null && activeSelectedTier.highlights.isNotBlank()) {
            activeSelectedTier?.let { tier ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, SecurityCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SecurityBlueLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tier.title}: ${tier.highlights}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = NeutralTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityServiceGrid(
    services: List<CommandCenterService>,
    selectedService: CommandCenterService?,
    onServiceSelected: (CommandCenterService) -> Unit,
    modifier: Modifier = Modifier,
    selectedTierTitle: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "service_pulse_anim")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!selectedTierTitle.isNullOrBlank()) {
                        "2. $selectedTierTitle Service Packages"
                    } else {
                        "2. Select Service Package"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = SecurityBluePrimary
                )
                Text(
                    text = "Live packages & rates from Command Center",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PurpleSurfaceTint,
                border = BorderStroke(1.dp, PurpleBorderTint)
            ) {
                Text(
                    text = "${services.size} ${if (services.size == 1) "Service" else "Services"}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecurityBlueLight
                )
            }
        }

        if (services.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SecurityCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No active services available for this tier yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val chunkedServices = services.chunked(2)

            chunkedServices.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { service ->
                        val isSelected = selectedService?.id == service.id && selectedService.code == service.code
                        val icon = getServiceIcon(service)
                        val resolvedBadge = service.badgeLabel.ifBlank {
                            if (service.isArmed) "ARMED" else "STANDARD"
                        }

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1.0f,
                            animationSpec = tween(durationMillis = 250),
                            label = "card_scale"
                        )
                        val animatedContainerColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> PurpleSurfaceTint
                                service.isRecommended -> Color(0xFFFAF8FF)
                                else -> Color.White
                            },
                            animationSpec = tween(durationMillis = 250),
                            label = "card_container_color"
                        )
                        val animatedBorderColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> SecurityBlueLight
                                service.isRecommended -> PurpleBorderTint
                                else -> SecurityCardBorder
                            },
                            animationSpec = tween(durationMillis = 250),
                            label = "card_border_color"
                        )
                        val animatedBorderWidth by animateDpAsState(
                            targetValue = if (isSelected) 2.dp else if (service.isRecommended) 1.4.dp else 1.dp,
                            animationSpec = tween(durationMillis = 250),
                            label = "card_border_width"
                        )
                        val animatedIconBg by animateColorAsState(
                            targetValue = if (isSelected || service.isRecommended) {
                                SecurityBlueLight.copy(alpha = 0.14f)
                            } else {
                                Color(0xFFF1F5F9)
                            },
                            animationSpec = tween(durationMillis = 250),
                            label = "icon_bg_color"
                        )
                        val animatedIconTint by animateColorAsState(
                            targetValue = if (isSelected || service.isRecommended) SecurityBlueLight else NeutralTextSecondary,
                            animationSpec = tween(durationMillis = 250),
                            label = "icon_tint_color"
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .scale(animatedScale)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onServiceSelected(service) }
                                .testTag("service_tile_${service.code.lowercase()}"),
                            colors = CardDefaults.cardColors(containerColor = animatedContainerColor),
                            border = BorderStroke(
                                width = animatedBorderWidth,
                                color = animatedBorderColor
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 1.dp
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (service.isRecommended) {
                                    PurpleStripedBanner(
                                        height = 4.dp,
                                        stripeWidthDp = 7f,
                                        gapWidthDp = 7f
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(animatedIconBg)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) SecurityBlueLight.copy(alpha = pulseGlowAlpha) else PurpleBorderTint,
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = service.title,
                                                tint = animatedIconTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = SecurityBlueLight,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Text(
                                                text = service.priceLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TacticalGreen
                                            )
                                        }
                                    }

                                    if (service.isRecommended) {
                                        PurpleStripedRecommendedBadge(
                                            text = "RECOMMENDED",
                                            modifier = Modifier.testTag("recommended_service_badge_${service.code.lowercase()}")
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = service.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = NeutralTextPrimary,
                                            fontSize = 14.sp
                                        )

                                        if (service.subtitle.isNotBlank()) {
                                            Text(
                                                text = service.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeutralTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                text = service.priceLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TacticalGreen
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (service.isArmed) EmergencyRed.copy(alpha = 0.12f) else SecurityBlueLight.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = resolvedBadge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (service.isArmed) EmergencyRed else SecurityBlueLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pair.size < 2 && services.size > 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun getServiceIcon(service: CommandCenterService): ImageVector {
    val key = service.code.uppercase()
    return when {
        key.contains("EVENT") || key.contains("CROWD") -> Icons.Default.Group
        key.contains("ARMED_GUARD") -> Icons.Default.Lock
        key.contains("BODYGUARD") || key.contains("EXECUTIVE") -> Icons.Default.Person
        key.contains("CASH") || key.contains("TRANSIT") -> Icons.Default.LocalShipping
        key.contains("RESPONSE") || key.contains("SOS") || key.contains("PANIC") -> Icons.Default.FlashOn
        key.contains("ESCORT") || key.contains("GPS") -> Icons.Default.GpsFixed
        else -> Icons.Default.Shield
    }
}
