package com.example.ui.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GpsCoordinateFix
import com.example.data.location.PanicLocationProvider
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest
import com.example.ui.components.CuttingEdgePanel
import com.example.ui.components.PurpleStripedRecommendedBadge
import com.example.ui.components.ResponsiveActionButton
import com.example.ui.components.ResponsiveDualButtonBar
import com.example.ui.components.ResponsiveOutlinedButton
import com.example.ui.components.ThreeDAnimatedIcon
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleStripeBright
import com.example.ui.theme.PurpleStripeGlow
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng

@Composable
internal fun NearbyGuardsQuickStrip(
    guards: List<NearbySecurityGuardMarker>,
    selectedGuardId: String?,
    onSelectGuard: (NearbySecurityGuardMarker) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nearby_guards_strip"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(guards, key = { it.id }) { guard ->
            val isSelected = guard.id == selectedGuardId
            val accent = when {
                guard.isEmergency -> EmergencyRed
                guard.isRecommended || isSelected -> PurplePrimary
                else -> TacticalGreen
            }
            CuttingEdgePanel(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 230.dp)
                    .testTag("nearby_guard_chip_${guard.id}"),
                shape = RoundedCornerShape(14.dp),
                containerColor = if (isSelected) PurpleSurfaceTint else Color.White.copy(alpha = 0.96f),
                borderColor = if (isSelected) accent else SecurityCardBorder,
                hasAnimatedBorder = isSelected,
                elevation = if (isSelected) 8.dp else 4.dp,
                onClick = { onSelectGuard(guard) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThreeDAnimatedIcon(
                        imageVector = Icons.Default.Security,
                        containerSize = 34.dp,
                        iconSize = 16.dp,
                        backgroundColors = listOf(accent, accent.copy(alpha = 0.8f)),
                        glowColor = accent,
                        shape = CircleShape,
                        animated = isSelected
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guard.guardName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${guard.distanceLabel} • ${guard.statusLabel}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
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
internal fun LiveMapStandbyCard(
    userLatLng: LatLng?,
    liveGpsFix: GpsCoordinateFix?,
    isRefreshingGps: Boolean,
    hasLocationPermission: Boolean,
    nearbyGuardsCount: Int,
    onRefreshGps: () -> Unit,
    onRequestProtection: () -> Unit
) {
    val coordText = userLatLng?.let {
        PanicLocationProvider.formatCoordinates(it.latitude, it.longitude)
    } ?: "GPS Pending"

    CuttingEdgePanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        borderColor = SecurityCardBorder,
        hasAnimatedBorder = true,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThreeDAnimatedIcon(
                    imageVector = Icons.Default.MyLocation,
                    containerSize = 44.dp,
                    iconSize = 22.dp,
                    backgroundColors = listOf(PurpleStripeBright, PurplePrimary),
                    glowColor = PurpleStripeGlow,
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nearbyGuardsCount > 0) {
                            "$nearbyGuardsCount Nearby Security Unit${if (nearbyGuardsCount == 1) "" else "s"}"
                        } else {
                            "Live Perimeter Standby"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NeutralTextPrimary
                    )
                    Text(
                        text = when {
                            isRefreshingGps -> "Acquiring high-accuracy GPS fix..."
                            liveGpsFix?.isLiveGps == true -> liveGpsFix.addressLabel
                            hasLocationPermission -> "GPS ready • Tap Sync GPS to update coordinates"
                            else -> "Enable location permission for live GPS tracking"
                        },
                        fontSize = 12.sp,
                        color = NeutralTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, SecurityCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text("GPS Coordinates", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = coordText,
                        fontSize = 12.sp,
                        color = SecurityBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SecurityCardBorder))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                    Text("Nearby Guards", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (nearbyGuardsCount > 0) "$nearbyGuardsCount Active" else "Awaiting Dispatch",
                        fontSize = 12.sp,
                        color = if (nearbyGuardsCount > 0) TacticalGreen else SecurityBlueLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ResponsiveDualButtonBar(
                primaryContent = { btnMod ->
                    ResponsiveActionButton(
                        text = "Request Guard",
                        icon = Icons.Default.Navigation,
                        onClick = onRequestProtection,
                        modifier = btnMod.testTag("btn_dispatch_unit")
                    )
                },
                secondaryContent = { btnMod ->
                    ResponsiveOutlinedButton(
                        text = if (isRefreshingGps) "Syncing..." else "Sync GPS",
                        icon = Icons.Default.MyLocation,
                        accentColor = TacticalGreen,
                        enabled = !isRefreshingGps,
                        onClick = onRefreshGps,
                        modifier = btnMod.testTag("btn_sync_live_gps")
                    )
                }
            )
        }
    }
}

@Composable
internal fun ActiveMissionMapDossierCard(
    activeRequest: SecurityRequest?,
    selectedGuard: NearbySecurityGuardMarker?,
    matchedGuardProfile: SecurityGuardProfile?,
    distanceDisplay: String,
    onViewGuardProfile: ((SecurityGuardProfile) -> Unit)?,
    onNavigateToRadar: (() -> Unit)?,
    onNewRequest: () -> Unit
) {
    val resolvedName = selectedGuard?.guardName
        ?: matchedGuardProfile?.name
        ?: activeRequest?.guardName?.takeIf { it.isNotBlank() }
        ?: "Awaiting Command Center..."
    val hasAssignedGuard = selectedGuard != null || !activeRequest?.guardName.isNullOrBlank()
    val rawStatus = selectedGuard?.status ?: activeRequest?.status
    val statusLabel = RadarTelemetryCalculator.formatStatusLabel(rawStatus)
    val statusColor = when (rawStatus) {
        "ON_SITE", "COMPLETED" -> TacticalGreen
        "CANCELLED" -> EmergencyRed
        "DISPATCHED", "EN_ROUTE" -> SecurityBlueLight
        else -> GoldLight
    }
    val etaMinutes = selectedGuard?.etaMinutes ?: activeRequest?.etaMinutes
    val etaDisplay = RadarTelemetryCalculator.formatDynamicEtaDisplay(
        distanceMeters = selectedGuard?.distanceMeters,
        status = rawStatus,
        baseEtaMinutes = etaMinutes
    )
    val resolvedProfile = selectedGuard?.profile ?: matchedGuardProfile
    val resolvedRating = selectedGuard?.rating?.toFloat()
        ?: matchedGuardProfile?.rating
        ?: 0f
    val isRecommended = selectedGuard?.isRecommended == true || matchedGuardProfile?.isRecommended == true
    val resolvedVehicle = selectedGuard?.vehicle ?: matchedGuardProfile?.vehicle
    val resolvedBadge = selectedGuard?.badgeNumber ?: matchedGuardProfile?.badgeNumber ?: activeRequest?.guardBadge
    val resolvedService = selectedGuard?.serviceType ?: activeRequest?.serviceName ?: "Security Detail"

    CuttingEdgePanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        borderColor = SecurityCardBorder,
        isRecommended = isRecommended,
        hasAnimatedBorder = !isRecommended,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThreeDAnimatedIcon(
                    imageVector = Icons.Default.Security,
                    containerSize = 48.dp,
                    iconSize = 24.dp,
                    backgroundColors = if (selectedGuard?.isEmergency == true || activeRequest?.isEmergency == true) {
                        listOf(EmergencyRed, Color(0xFF991B1B))
                    } else {
                        listOf(TacticalGreen, Color(0xFF047857))
                    },
                    glowColor = if (selectedGuard?.isEmergency == true || activeRequest?.isEmergency == true) {
                        EmergencyRed
                    } else {
                        TacticalGreen
                    },
                    shape = CircleShape
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = resolvedName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (resolvedRating > 0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$resolvedRating",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight
                                    )
                                }
                            }
                        }
                    }

                    if (isRecommended) {
                        Spacer(modifier = Modifier.height(3.dp))
                        PurpleStripedRecommendedBadge(text = "RECOMMENDED")
                    }

                    Text(
                        text = if (hasAssignedGuard) {
                            "$resolvedService • Badge ${resolvedBadge ?: "Verified"}"
                        } else {
                            "$resolvedService • Command Center Queue"
                        },
                        fontSize = 12.sp,
                        color = NeutralTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!resolvedVehicle.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = SecurityBlueLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = resolvedVehicle,
                                fontSize = 11.sp,
                                color = SecurityBlueLight,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (resolvedProfile != null && onViewGuardProfile != null) {
                    Surface(
                        onClick = { onViewGuardProfile(resolvedProfile) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_view_guard_profile"),
                        shape = RoundedCornerShape(10.dp),
                        color = PurpleSurfaceTint,
                        border = BorderStroke(1.dp, SecurityBlueLight.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecurityBlueLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, SecurityCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Estimated ETA", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(text = etaDisplay, fontSize = 14.sp, color = TacticalGreen, fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SecurityCardBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Distance", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = selectedGuard?.distanceLabel ?: distanceDisplay,
                        fontSize = 14.sp,
                        color = SecurityBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SecurityCardBorder))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Unit Status", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = statusLabel,
                        fontSize = 13.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (onNavigateToRadar != null) {
                ResponsiveDualButtonBar(
                    primaryContent = { btnMod ->
                        ResponsiveActionButton(
                            text = "Open Radar",
                            icon = Icons.Default.GpsFixed,
                            onClick = onNavigateToRadar,
                            modifier = btnMod.testTag("btn_open_radar_from_map")
                        )
                    },
                    secondaryContent = { btnMod ->
                        ResponsiveOutlinedButton(
                            text = "New Dispatch",
                            icon = Icons.Default.Navigation,
                            accentColor = SecurityBlueLight,
                            onClick = onNewRequest,
                            modifier = btnMod.testTag("btn_dispatch_unit")
                        )
                    }
                )
            } else {
                ResponsiveActionButton(
                    text = "New Dispatch",
                    icon = Icons.Default.Navigation,
                    onClick = onNewRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_dispatch_unit")
                )
            }
        }
    }
}

/**
 * Creates a custom marker bitmap icon for Google Map markers.
 */
@Composable
internal fun rememberCustomMarkerIcon(
    context: Context,
    colorLong: Long,
    label: String
): BitmapDescriptor {
    return remember(colorLong, label) {
        val density = context.resources.displayMetrics.density
        val sizePx = (40 * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintCircle = Paint().apply {
            color = colorLong.toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val paintBorder = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3 * density
        }

        val paintText = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            textSize = 10 * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val center = sizePx / 2f
        val radius = (sizePx / 2f) - (3 * density)

        canvas.drawCircle(center, center, radius, paintCircle)
        canvas.drawCircle(center, center, radius, paintBorder)
        canvas.drawText(label, center, center + (3 * density), paintText)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
