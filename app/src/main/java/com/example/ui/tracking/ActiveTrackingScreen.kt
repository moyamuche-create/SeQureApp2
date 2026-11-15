package com.example.ui.tracking

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GpsCoordinateFix
import com.example.data.location.PanicLocationProvider
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest
import com.example.ui.components.ResponsiveActionButton
import com.example.ui.components.ResponsiveDualButtonBar
import com.example.ui.components.ResponsiveOutlinedButton
import com.example.ui.components.ResponsiveSegmentButton
import com.example.ui.dispatch.RadarTrackingCanvas
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import kotlinx.coroutines.launch

@Composable
fun ActiveTrackingScreen(
    activeRequest: SecurityRequest?,
    activeDeployment: CommandCenterDeployment? = null,
    onCancelRequest: (SecurityRequest) -> Unit,
    onNavigateToBooking: () -> Unit,
    onNavigateToLiveMap: () -> Unit = {},
    onViewGuardProfile: ((SecurityGuardProfile) -> Unit)? = null,
    onSubmitFeedback: ((requestId: String, rating: Int, comment: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    var selectedRangeMeters by remember { mutableDoubleStateOf(1000.0) }
    var liveGpsFix by remember { mutableStateOf<GpsCoordinateFix?>(null) }
    var isRefreshingGps by remember { mutableStateOf(false) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                isRefreshingGps = true
                liveGpsFix = PanicLocationProvider.acquireCurrentGpsLocation(
                    context = context,
                    fallbackLocationName = activeRequest?.locationName
                )
                isRefreshingGps = false
            }
        }
    }

    val refreshLiveGps: () -> Unit = {
        if (!isRefreshingGps) {
            if (PanicLocationProvider.hasLocationPermission(context)) {
                coroutineScope.launch {
                    isRefreshingGps = true
                    liveGpsFix = PanicLocationProvider.acquireCurrentGpsLocation(
                        context = context,
                        fallbackLocationName = activeRequest?.locationName
                    )
                    isRefreshingGps = false
                }
            } else {
                gpsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (liveGpsFix == null && PanicLocationProvider.hasLocationPermission(context)) {
            isRefreshingGps = true
            liveGpsFix = PanicLocationProvider.acquireCurrentGpsLocation(
                context = context,
                fallbackLocationName = activeRequest?.locationName
            )
            isRefreshingGps = false
        }
    }

    if (activeRequest == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("no_active_tracking_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RadarTrackingCanvas(
                    status = "STANDBY",
                    isEmergency = false,
                    etaMinutes = null,
                    guardName = null,
                    guardBadge = null,
                    guardDistanceFactor = 0.0f,
                    rangeMeters = selectedRangeMeters,
                    distanceLabel = "Standby",
                    bearingLabel = if (liveGpsFix?.isLiveGps == true) "GPS Locked" else "Perimeter Ready",
                    coordinatesLabel = liveGpsFix?.let {
                        PanicLocationProvider.formatCoordinates(it.latitude, it.longitude)
                    }
                )

                Text(
                    text = "Radar Standby • No Active Dispatch",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NeutralTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Request immediate or scheduled protection to lock onto your assigned security unit on live radar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralTextSecondary,
                    textAlign = TextAlign.Center
                )

                ResponsiveDualButtonBar(
                    primaryContent = { btnMod ->
                        ResponsiveActionButton(
                            text = "Request Guard",
                            icon = Icons.Default.Security,
                            onClick = onNavigateToBooking,
                            modifier = btnMod.testTag("request_dispatch_cta")
                        )
                    },
                    secondaryContent = { btnMod ->
                        ResponsiveOutlinedButton(
                            text = "Open Live Map",
                            icon = Icons.Default.Map,
                            onClick = onNavigateToLiveMap,
                            modifier = btnMod.testTag("radar_empty_open_map_cta")
                        )
                    }
                )

                ResponsiveOutlinedButton(
                    text = if (isRefreshingGps) "Acquiring GPS..." else "Scan Current GPS",
                    icon = Icons.Default.MyLocation,
                    accentColor = TacticalGreen,
                    onClick = refreshLiveGps,
                    enabled = !isRefreshingGps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radar_standby_scan_gps")
                )

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
        return
    }

    // Merge fresh user GPS fix & live Command Center deployment coordinates
    val effectiveRequest = remember(activeRequest, activeDeployment, liveGpsFix) {
        val fix = liveGpsFix
        val mergedGuardLat = activeDeployment?.guardLat ?: activeRequest.guardLat
        val mergedGuardLng = activeDeployment?.guardLng ?: activeRequest.guardLng
        val mergedGuardName = activeDeployment?.guardName?.takeIf { it.isNotBlank() } ?: activeRequest.guardName
        val mergedGuardBadge = activeDeployment?.badgeNumber?.takeIf { it.isNotBlank() } ?: activeRequest.guardBadge
        val mergedGuardPhone = activeDeployment?.phone?.takeIf { it.isNotBlank() } ?: activeRequest.guardPhone
        activeRequest.copy(
            latitude = if (fix != null && fix.isLiveGps) fix.latitude else activeRequest.latitude,
            longitude = if (fix != null && fix.isLiveGps) fix.longitude else activeRequest.longitude,
            guardName = mergedGuardName,
            guardBadge = mergedGuardBadge,
            guardPhone = mergedGuardPhone,
            guardLat = mergedGuardLat,
            guardLng = mergedGuardLng
        )
    }

    val matchedGuardProfile = remember(effectiveRequest, activeDeployment) {
        SecurityGuardProfile.fromCommandCenter(effectiveRequest, activeDeployment)
    }

    val telemetry = remember(effectiveRequest, selectedRangeMeters) {
        RadarTelemetryCalculator.computeSnapshot(effectiveRequest, selectedRangeMeters)
    }
    val hasValidCoords = effectiveRequest.latitude != 0.0 || effectiveRequest.longitude != 0.0
    val coordLabel = if (hasValidCoords) {
        PanicLocationProvider.formatCoordinates(effectiveRequest.latitude, effectiveRequest.longitude)
    } else {
        null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("active_tracking_screen")
    ) {
        val isWide = maxWidth >= 650.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TopMissionBanner(effectiveRequest)
                    RadarTrackingCanvas(
                        status = effectiveRequest.status,
                        isEmergency = effectiveRequest.isEmergency,
                        etaMinutes = effectiveRequest.etaMinutes,
                        guardName = effectiveRequest.guardName,
                        guardBadge = effectiveRequest.guardBadge,
                        guardDistanceFactor = telemetry.normalizedRadius,
                        guardBearingDegrees = telemetry.bearingDegrees,
                        rangeMeters = selectedRangeMeters,
                        distanceLabel = telemetry.distanceLabel,
                        bearingLabel = telemetry.bearingCompassLabel,
                        coordinatesLabel = coordLabel
                    )
                    RadarRangeAndTelemetryCard(
                        selectedRangeMeters = selectedRangeMeters,
                        onRangeSelected = { selectedRangeMeters = it },
                        telemetry = telemetry,
                        coordinatesLabel = coordLabel ?: effectiveRequest.locationName,
                        isRefreshingGps = isRefreshingGps,
                        onRefreshGps = refreshLiveGps
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PipelineStepperCard(effectiveRequest)
                    if (effectiveRequest.isCompleted && onSubmitFeedback != null) {
                        PostServiceFeedbackCard(
                            request = effectiveRequest,
                            onSubmitFeedback = { rating, comment ->
                                onSubmitFeedback(effectiveRequest.id, rating, comment)
                            }
                        )
                    }
                    AssignedOfficerCard(
                        activeRequest = effectiveRequest,
                        matchedGuardProfile = matchedGuardProfile,
                        context = context,
                        onViewGuardProfile = onViewGuardProfile
                    )
                    RequestActionControls(
                        activeRequest = effectiveRequest,
                        onNavigateToLiveMap = onNavigateToLiveMap,
                        onShowCancelDialog = { showCancelDialog = true },
                        onNavigateToBooking = onNavigateToBooking
                    )
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { TopMissionBanner(effectiveRequest) }
                item {
                    RadarTrackingCanvas(
                        status = effectiveRequest.status,
                        isEmergency = effectiveRequest.isEmergency,
                        etaMinutes = effectiveRequest.etaMinutes,
                        guardName = effectiveRequest.guardName,
                        guardBadge = effectiveRequest.guardBadge,
                        guardDistanceFactor = telemetry.normalizedRadius,
                        guardBearingDegrees = telemetry.bearingDegrees,
                        rangeMeters = selectedRangeMeters,
                        distanceLabel = telemetry.distanceLabel,
                        bearingLabel = telemetry.bearingCompassLabel,
                        coordinatesLabel = coordLabel
                    )
                }
                item {
                    RadarRangeAndTelemetryCard(
                        selectedRangeMeters = selectedRangeMeters,
                        onRangeSelected = { selectedRangeMeters = it },
                        telemetry = telemetry,
                        coordinatesLabel = coordLabel ?: effectiveRequest.locationName,
                        isRefreshingGps = isRefreshingGps,
                        onRefreshGps = refreshLiveGps
                    )
                }
                item { PipelineStepperCard(effectiveRequest) }
                if (effectiveRequest.isCompleted && onSubmitFeedback != null) {
                    item {
                        PostServiceFeedbackCard(
                            request = effectiveRequest,
                            onSubmitFeedback = { rating, comment ->
                                onSubmitFeedback(effectiveRequest.id, rating, comment)
                            }
                        )
                    }
                }
                item {
                    AssignedOfficerCard(
                        activeRequest = effectiveRequest,
                        matchedGuardProfile = matchedGuardProfile,
                        context = context,
                        onViewGuardProfile = onViewGuardProfile
                    )
                }
                item {
                    RequestActionControls(
                        activeRequest = effectiveRequest,
                        onNavigateToLiveMap = onNavigateToLiveMap,
                        onShowCancelDialog = { showCancelDialog = true },
                        onNavigateToBooking = onNavigateToBooking
                    )
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel Security Mission?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NeutralTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel the dispatch for ${effectiveRequest.serviceName}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralTextSecondary
                )
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelRequest(effectiveRequest)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_cancel_button")
                ) {
                    Text("Yes, Cancel Dispatch", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Keep Active", color = SecurityBlueLight, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun RadarRangeAndTelemetryCard(
    selectedRangeMeters: Double,
    onRangeSelected: (Double) -> Unit,
    telemetry: RadarTelemetrySnapshot,
    coordinatesLabel: String,
    isRefreshingGps: Boolean,
    onRefreshGps: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = SecurityBlueLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Radar Range & GPS Lock",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SecurityBluePrimary
                    )
                }

                ResponsiveOutlinedButton(
                    text = if (isRefreshingGps) "Syncing..." else "Sync GPS",
                    icon = Icons.Default.MyLocation,
                    accentColor = TacticalGreen,
                    onClick = onRefreshGps,
                    enabled = !isRefreshingGps,
                    minHeight = 48.dp,
                    modifier = Modifier.testTag("radar_sync_gps_button")
                )
            }

            // Responsive Radar Range Switcher (500m / 1km / 3km)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ranges = listOf(
                    500.0 to "500 m",
                    1000.0 to "1 km",
                    3000.0 to "3 km"
                )
                ranges.forEach { (meters, label) ->
                    ResponsiveSegmentButton(
                        label = label,
                        selected = selectedRangeMeters == meters,
                        onClick = { onRangeSelected(meters) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("radar_range_${meters.toInt()}")
                    )
                }
            }

            // Live Distance, Bearing, & Coordinates Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, SecurityCardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text("Distance", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = telemetry.distanceLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalGreen
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SecurityCardBorder))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bearing", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = telemetry.bearingCompassLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecurityBluePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(SecurityCardBorder))
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1.3f)
                ) {
                    Text("GPS Fix", fontSize = 10.sp, color = NeutralTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        text = coordinatesLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeutralTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TopMissionBanner(activeRequest: SecurityRequest) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (activeRequest.isEmergency) Color(0xFFFEF2F2) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (activeRequest.isEmergency) EmergencyRed.copy(alpha = 0.6f) else SecurityCardBorder,
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeRequest.isEmergency) EmergencyRed.copy(alpha = 0.12f)
                            else TacticalGreen.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeRequest.isEmergency) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (activeRequest.isEmergency) EmergencyRed else TacticalGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = if (activeRequest.isEmergency) "Emergency SOS Priority" else activeRequest.serviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = NeutralTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Mission ID: ${activeRequest.id.take(8).ifBlank { "PENDING" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralTextSecondary
                    )
                }
            }

            StatusBadge(status = activeRequest.status)
        }
    }
}

@Composable
private fun PipelineStepperCard(activeRequest: SecurityRequest) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Dispatch Progress",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = SecurityBluePrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            val stages = listOf(
                "REQUESTED" to "Req",
                "DISPATCHED" to "Assigned",
                "EN_ROUTE" to "En Route",
                "ON_SITE" to "On Site",
                "COMPLETED" to "Done"
            )

            val currentIndex = when (activeRequest.status) {
                "REQUESTED" -> 0
                "DISPATCHED" -> 1
                "EN_ROUTE" -> 2
                "ON_SITE" -> 3
                "COMPLETED" -> 4
                else -> 0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEachIndexed { index, (_, label) ->
                    val isPassed = index <= currentIndex
                    val isCurrent = index == currentIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (index == 0) 0.dp else 2.5.dp)
                                    .background(
                                        if (index > 0 && index <= currentIndex) TacticalGreen
                                        else Color(0xFFE2E8F0)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> SecurityBlueLight
                                            isPassed -> TacticalGreen
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPassed && !isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else NeutralTextSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (index == stages.lastIndex) 0.dp else 2.5.dp)
                                    .background(
                                        if (index < currentIndex) TacticalGreen
                                        else Color(0xFFE2E8F0)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isCurrent -> SecurityBlueLight
                                isPassed -> TacticalGreen
                                else -> NeutralTextMuted
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignedOfficerCard(
    activeRequest: SecurityRequest,
    matchedGuardProfile: SecurityGuardProfile?,
    context: android.content.Context,
    onViewGuardProfile: ((SecurityGuardProfile) -> Unit)?
) {
    val hasAssignedGuard = !activeRequest.guardName.isNullOrBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
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
                    text = "Assigned Security Officer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SecurityBluePrimary
                )

                if (hasAssignedGuard && matchedGuardProfile != null && onViewGuardProfile != null) {
                    Surface(
                        onClick = { onViewGuardProfile(matchedGuardProfile) },
                        shape = RoundedCornerShape(8.dp),
                        color = PurpleSurfaceTint,
                        border = BorderStroke(1.dp, SecurityBlueLight.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("radar_view_guard_profile")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "View Profile",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SecurityBlueLight
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PurpleSurfaceTint)
                            .border(1.5.dp, SecurityBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SecurityBlueLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = activeRequest.guardName?.takeIf { it.isNotBlank() }
                                ?: "Awaiting Command Center...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasAssignedGuard) {
                                "Badge: ${activeRequest.guardBadge ?: "Verified"} • Assigned by Command Center"
                            } else {
                                "Command Center Queue • Officer assignment pending"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary
                        )
                    }
                }

                if (!activeRequest.guardPhone.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${activeRequest.guardPhone}"))
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                // Safe no-op on devices without a dialer app
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TacticalGreen.copy(alpha = 0.12f))
                            .border(1.dp, TacticalGreen.copy(alpha = 0.5f), CircleShape)
                            .testTag("call_guard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Assigned Guard",
                            tint = TacticalGreen
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = SecurityBlueLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Site: ${activeRequest.locationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RequestActionControls(
    activeRequest: SecurityRequest,
    onNavigateToLiveMap: () -> Unit,
    onShowCancelDialog: () -> Unit,
    onNavigateToBooking: () -> Unit
) {
    if (activeRequest.status != "COMPLETED" && activeRequest.status != "CANCELLED") {
        ResponsiveDualButtonBar(
            primaryContent = { btnModifier ->
                ResponsiveActionButton(
                    text = "Open Live Map",
                    icon = Icons.Default.Map,
                    onClick = onNavigateToLiveMap,
                    modifier = btnModifier.testTag("radar_open_live_map_button")
                )
            },
            secondaryContent = { btnModifier ->
                ResponsiveOutlinedButton(
                    text = "Cancel Request",
                    icon = Icons.Default.Close,
                    accentColor = EmergencyRed,
                    onClick = onShowCancelDialog,
                    modifier = btnModifier.testTag("cancel_request_button")
                )
            }
        )
    } else {
        ResponsiveDualButtonBar(
            primaryContent = { btnModifier ->
                ResponsiveActionButton(
                    text = "Request Another Service",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToBooking,
                    modifier = btnModifier.testTag("book_another_service_button")
                )
            },
            secondaryContent = { btnModifier ->
                ResponsiveOutlinedButton(
                    text = "View on Map",
                    icon = Icons.Default.Map,
                    onClick = onNavigateToLiveMap,
                    modifier = btnModifier.testTag("radar_open_live_map_button")
                )
            }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, text, label) = when (status) {
        "REQUESTED" -> Triple(PurpleSurfaceTint, SecurityBlueLight, "Dispatching")
        "DISPATCHED" -> Triple(Color(0xFFFEF3C7), GoldLight, "Assigned")
        "EN_ROUTE" -> Triple(Color(0xFFFEF3C7), GoldLight, "En Route")
        "ON_SITE" -> Triple(Color(0xFFECFDF5), TacticalGreen, "On Site")
        "COMPLETED" -> Triple(Color(0xFFECFDF5), TacticalGreen, "Completed")
        "CANCELLED" -> Triple(Color(0xFFFEF2F2), EmergencyRed, "Cancelled")
        else -> Triple(Color(0xFFF1F5F9), NeutralTextSecondary, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, text.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = text
        )
    }
}
