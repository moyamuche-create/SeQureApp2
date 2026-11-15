package com.example.ui.tracking

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.GpsCoordinateFix
import com.example.data.location.PanicLocationProvider
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest
import com.example.service.MapTileCacheManager
import com.example.ui.components.CuttingEdgePanel
import com.example.ui.components.ResponsiveSegmentButton
import com.example.ui.components.ThreeDAnimatedIcon
import com.example.ui.components.threeDTactilePush
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleStripeBright
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

/**
 * Live Google Maps screen displaying the user's real GPS location and
 * all nearby security guards dispatched or deployed from Command Center.
 */
@Composable
fun MapScreen(
    nearbyGuards: List<CommandCenterDeployment> = emptyList(),
    activeRequest: SecurityRequest? = null,
    activeDeployment: CommandCenterDeployment? = null,
    onNavigateToRadar: (() -> Unit)? = null,
    onDispatchGuardRequested: ((String) -> Unit)? = null,
    onViewGuardProfile: ((SecurityGuardProfile) -> Unit)? = null,
    onSubmitFeedback: ((requestId: String, rating: Int, comment: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(PanicLocationProvider.hasLocationPermission(context))
    }
    var liveGpsFix by remember { mutableStateOf<GpsCoordinateFix?>(null) }
    var isRefreshingGps by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        val initialLat = activeRequest?.latitude?.takeIf { it != 0.0 } ?: PanicLocationProvider.UNSET_LAT
        val initialLng = activeRequest?.longitude?.takeIf { it != 0.0 } ?: PanicLocationProvider.UNSET_LNG
        val initialZoom = if (initialLat != 0.0 || initialLng != 0.0) 15f else 2f
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), initialZoom)
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            coroutineScope.launch {
                isRefreshingGps = true
                val fix = PanicLocationProvider.acquireCurrentGpsLocation(
                    context = context,
                    fallbackLocationName = activeRequest?.locationName
                )
                liveGpsFix = fix
                isRefreshingGps = false
                if (fix.isLiveGps) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), 16f),
                        700
                    )
                }
            }
        }
    }

    val refreshLiveGpsAndCenter: () -> Unit = {
        if (!isRefreshingGps) {
            if (PanicLocationProvider.hasLocationPermission(context)) {
                hasLocationPermission = true
                coroutineScope.launch {
                    isRefreshingGps = true
                    val fix = PanicLocationProvider.acquireCurrentGpsLocation(
                        context = context,
                        fallbackLocationName = activeRequest?.locationName
                    )
                    liveGpsFix = fix
                    isRefreshingGps = false
                    val target = if (fix.isLiveGps) {
                        LatLng(fix.latitude, fix.longitude)
                    } else {
                        val reqLat = activeRequest?.latitude ?: 0.0
                        val reqLng = activeRequest?.longitude ?: 0.0
                        if (reqLat != 0.0 || reqLng != 0.0) LatLng(reqLat, reqLng) else null
                    }
                    if (target != null) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(target, 16f),
                            800
                        )
                    }
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
            hasLocationPermission = true
            isRefreshingGps = true
            liveGpsFix = PanicLocationProvider.acquireCurrentGpsLocation(
                context = context,
                fallbackLocationName = activeRequest?.locationName
            )
            isRefreshingGps = false
        }
    }

    // Prioritize real live GPS fix, then active request coordinates; null until resolved
    val userLatLng = remember(liveGpsFix, activeRequest?.latitude, activeRequest?.longitude) {
        val fix = liveGpsFix
        val reqLat = activeRequest?.latitude ?: 0.0
        val reqLng = activeRequest?.longitude ?: 0.0
        when {
            fix != null && fix.isLiveGps -> LatLng(fix.latitude, fix.longitude)
            reqLat != 0.0 || reqLng != 0.0 -> LatLng(reqLat, reqLng)
            else -> null
        }
    }

    val trackedGuards = remember(nearbyGuards, activeRequest, activeDeployment, userLatLng) {
        NearbyGuardsMapper.buildNearbyGuards(
            deployments = nearbyGuards,
            activeRequest = activeRequest,
            activeDeployment = activeDeployment,
            userLat = userLatLng?.latitude,
            userLng = userLatLng?.longitude
        )
    }

    var selectedGuardId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(trackedGuards) {
        if (trackedGuards.isEmpty()) {
            selectedGuardId = null
        } else if (selectedGuardId == null || trackedGuards.none { it.id == selectedGuardId }) {
            selectedGuardId = trackedGuards.first().id
        }
    }

    val selectedGuard = remember(trackedGuards, selectedGuardId) {
        trackedGuards.firstOrNull { it.id == selectedGuardId } ?: trackedGuards.firstOrNull()
    }
    val focusedGuardLatLng = remember(selectedGuard) {
        selectedGuard?.let { LatLng(it.latitude, it.longitude) }
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    LaunchedEffect(userLatLng) {
        userLatLng?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    val distanceDisplay = remember(selectedGuard, activeRequest?.status) {
        when {
            selectedGuard != null -> selectedGuard.distanceLabel
            activeRequest?.status == "ON_SITE" -> "On Site"
            activeRequest != null -> "Syncing GPS"
            else -> "--"
        }
    }

    val matchedGuardProfile = remember(activeRequest, activeDeployment, selectedGuard) {
        selectedGuard?.profile ?: SecurityGuardProfile.fromCommandCenter(activeRequest, activeDeployment)
    }

    var isConnected by remember { mutableStateOf(MapTileCacheManager.isNetworkConnected(context)) }
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isConnected = MapTileCacheManager.isNetworkConnected(context)
            }

            override fun onLost(network: Network) {
                isConnected = MapTileCacheManager.isNetworkConnected(context)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        if (connectivityManager != null) {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            try {
                connectivityManager.registerNetworkCallback(request, callback)
            } catch (_: Exception) {
                isConnected = MapTileCacheManager.isNetworkConnected(context)
            }
        }
        onDispose {
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                    // Ignore already-unregistered callback
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_view"),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            // User Current Location Perimeter & Custom Marker
            if (userLatLng != null) {
                Circle(
                    center = userLatLng,
                    radius = liveGpsFix?.accuracyMeters?.toDouble()?.coerceIn(45.0, 250.0) ?: 120.0,
                    strokeColor = if (activeRequest?.isEmergency == true) Color(0xFFEF4444) else Color(0xFF6D28D9),
                    strokeWidth = 3f,
                    fillColor = if (activeRequest?.isEmergency == true) Color(0x33EF4444) else Color(0x336D28D9)
                )

                val userMarkerState = rememberMarkerState(
                    key = "user_marker",
                    position = userLatLng
                )
                LaunchedEffect(userLatLng) {
                    userMarkerState.position = userLatLng
                }
                Marker(
                    state = userMarkerState,
                    title = when {
                        activeRequest?.isEmergency == true -> "SOS GPS LOCATION"
                        liveGpsFix?.isLiveGps == true -> "Your Live GPS"
                        else -> "Your Location"
                    },
                    snippet = liveGpsFix?.takeIf { it.isLiveGps }?.addressLabel
                        ?: activeRequest?.locationName?.takeIf { it.isNotBlank() }
                        ?: "Tap My Location to sync GPS",
                    icon = rememberCustomMarkerIcon(
                        context = context,
                        colorLong = if (activeRequest?.isEmergency == true) 0xFFDC2626 else 0xFF6D28D9,
                        label = if (activeRequest?.isEmergency == true) "SOS" else "YOU"
                    )
                )
            }

            // Nearby Security Guard Markers & Active Route Polyline
            trackedGuards.forEach { guard ->
                val guardPos = LatLng(guard.latitude, guard.longitude)
                val isSelected = guard.id == selectedGuard?.id
                val markerColor = when {
                    guard.isEmergency -> 0xFFDC2626
                    guard.isRecommended || isSelected -> 0xFF6D28D9
                    else -> 0xFF059669
                }
                val markerTag = when {
                    guard.isEmergency -> "SOS"
                    guard.isRecommended -> "TOP"
                    else -> "SEC"
                }

                if (isSelected) {
                    Circle(
                        center = guardPos,
                        radius = 75.0,
                        strokeColor = if (guard.isEmergency) EmergencyRed else PurplePrimary,
                        strokeWidth = 2.5f,
                        fillColor = if (guard.isEmergency) Color(0x22DC2626) else Color(0x226D28D9)
                    )
                }

                val guardMarkerState = rememberMarkerState(
                    key = "guard_${guard.id}",
                    position = guardPos
                )
                LaunchedEffect(guardPos) {
                    guardMarkerState.position = guardPos
                }

                Marker(
                    state = guardMarkerState,
                    title = guard.guardName,
                    snippet = "${guard.serviceType} • ${guard.distanceLabel} • ${guard.statusLabel}",
                    icon = rememberCustomMarkerIcon(
                        context = context,
                        colorLong = markerColor,
                        label = markerTag
                    ),
                    onClick = {
                        selectedGuardId = guard.id
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(guardPos, 16f),
                                600
                            )
                        }
                        false
                    }
                )
            }

            if (userLatLng != null && focusedGuardLatLng != null) {
                Polyline(
                    points = listOf(focusedGuardLatLng, userLatLng),
                    color = if (selectedGuard?.isEmergency == true) Color(0xFFDC2626) else Color(0xFF059669),
                    width = 8f,
                    geodesic = true
                )
            }
        }

        // Top Cutting-Edge Control Bar & Map Switcher Overlay
        CuttingEdgePanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White.copy(alpha = 0.96f),
            borderColor = SecurityCardBorder,
            hasAnimatedBorder = true,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConnected) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isConnected) TacticalGreen.copy(alpha = 0.4f) else Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) TacticalGreen else Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) {
                                "Map Tile Disk Cache Active • Surroundings Saved"
                            } else {
                                "Offline Mode • Local Map Tiles Active from Disk Cache"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnected) TacticalGreen else Color(0xFFB45309)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (activeRequest != null) {
                    if (activeRequest.isCompleted && onSubmitFeedback != null) {
                        PostServiceFeedbackCard(
                            request = activeRequest,
                            onSubmitFeedback = { rating, comment ->
                                onSubmitFeedback(activeRequest.id, rating, comment)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        ServiceStatusTrackerComponent(request = activeRequest)
                        Spacer(modifier = Modifier.height(10.dp))
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
                        ThreeDAnimatedIcon(
                            imageVector = Icons.Default.Security,
                            containerSize = 36.dp,
                            iconSize = 18.dp,
                            backgroundColors = if (activeRequest?.isEmergency == true) {
                                listOf(EmergencyRed, Color(0xFF991B1B))
                            } else {
                                listOf(PurplePrimary, PurpleStripeBright)
                            },
                            glowColor = if (activeRequest?.isEmergency == true) EmergencyRed else PurplePrimary,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Live Guard Map",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeutralTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (trackedGuards.isNotEmpty()) Color(0xFFECFDF5) else PurpleSurfaceTint,
                                    border = BorderStroke(
                                        1.dp,
                                        if (trackedGuards.isNotEmpty()) TacticalGreen.copy(alpha = 0.4f) else PurplePrimary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = if (trackedGuards.isNotEmpty()) "${trackedGuards.size} Nearby" else "Standby",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (trackedGuards.isNotEmpty()) TacticalGreen else PurplePrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val modeLabel = mapType.name.lowercase().replaceFirstChar { it.uppercase() }
                            val gpsStatus = when {
                                isRefreshingGps -> "Syncing GPS..."
                                liveGpsFix?.isLiveGps == true -> "GPS Locked"
                                activeRequest != null -> RadarTelemetryCalculator.formatStatusLabel(activeRequest.status)
                                else -> "Standby"
                            }
                            Text(
                                text = "$modeLabel view • $gpsStatus",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecurityBlueLight
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tactile3DMapActionButton(
                            icon = Icons.Default.Layers,
                            contentDescription = "Map Style",
                            tint = SecurityBluePrimary,
                            containerColor = Color(0xFFF1F5F9),
                            borderColor = SecurityCardBorder,
                            shape = RoundedCornerShape(10.dp),
                            testTag = "map_type_toggle",
                            onClick = {
                                mapType = when (mapType) {
                                    MapType.NORMAL -> MapType.SATELLITE
                                    MapType.SATELLITE -> MapType.HYBRID
                                    MapType.HYBRID -> MapType.TERRAIN
                                    else -> MapType.NORMAL
                                }
                            }
                        )

                        if (onNavigateToRadar != null) {
                            Tactile3DMapActionButton(
                                icon = Icons.Default.GpsFixed,
                                contentDescription = "Radar View",
                                tint = TacticalGreen,
                                containerColor = Color(0xFFECFDF5),
                                borderColor = TacticalGreen.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                testTag = "radar_canvas_toggle",
                                onClick = onNavigateToRadar
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val mapModes = listOf(
                    MapType.NORMAL to "Standard",
                    MapType.SATELLITE to "Satellite",
                    MapType.TERRAIN to "Terrain",
                    MapType.HYBRID to "Hybrid"
                )
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 340.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mapModes.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    pair.forEach { (type, label) ->
                                        ResponsiveSegmentButton(
                                            label = label,
                                            selected = mapType == type,
                                            onClick = { mapType = type },
                                            minHeight = 48.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("map_mode_chip_${label.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mapModes.forEach { (type, label) ->
                                ResponsiveSegmentButton(
                                    label = label,
                                    selected = mapType == type,
                                    onClick = { mapType = type },
                                    minHeight = 48.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("map_mode_chip_${label.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Stack: Camera Controls + Nearby Guard Quick-Select Strip + Dossier/Standby Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 78.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tactile3DMapActionButton(
                        icon = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        tint = SecurityBlueLight,
                        containerColor = Color.White.copy(alpha = 0.96f),
                        borderColor = SecurityBlueLight,
                        shape = CircleShape,
                        testTag = "btn_center_user",
                        onClick = refreshLiveGpsAndCenter
                    )

                    if (focusedGuardLatLng != null) {
                        Tactile3DMapActionButton(
                            icon = Icons.Default.Security,
                            contentDescription = "Nearest Guard",
                            tint = TacticalGreen,
                            containerColor = Color.White.copy(alpha = 0.96f),
                            borderColor = TacticalGreen,
                            shape = CircleShape,
                            testTag = "btn_center_guard",
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(focusedGuardLatLng, 16f),
                                        800
                                    )
                                }
                            }
                        )

                        if (userLatLng != null || trackedGuards.size > 1) {
                            Tactile3DMapActionButton(
                                icon = Icons.Default.Map,
                                contentDescription = "Fit Route",
                                tint = GoldLight,
                                containerColor = Color.White.copy(alpha = 0.96f),
                                borderColor = GoldAccent,
                                shape = CircleShape,
                                testTag = "btn_fit_bounds",
                                onClick = {
                                    coroutineScope.launch {
                                        val builder = LatLngBounds.builder()
                                        userLatLng?.let { builder.include(it) }
                                        trackedGuards.forEach { g ->
                                            builder.include(LatLng(g.latitude, g.longitude))
                                        }
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngBounds(builder.build(), 120),
                                            800
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (trackedGuards.isNotEmpty()) {
                NearbyGuardsQuickStrip(
                    guards = trackedGuards,
                    selectedGuardId = selectedGuard?.id,
                    onSelectGuard = { guard ->
                        selectedGuardId = guard.id
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(guard.latitude, guard.longitude),
                                    16f
                                ),
                                700
                            )
                        }
                    }
                )
            }

            if (activeRequest == null && selectedGuard == null) {
                LiveMapStandbyCard(
                    userLatLng = userLatLng,
                    liveGpsFix = liveGpsFix,
                    isRefreshingGps = isRefreshingGps,
                    hasLocationPermission = hasLocationPermission,
                    nearbyGuardsCount = trackedGuards.size,
                    onRefreshGps = refreshLiveGpsAndCenter,
                    onRequestProtection = {
                        onDispatchGuardRequested?.invoke("Request Protection")
                    }
                )
            } else {
                ActiveMissionMapDossierCard(
                    activeRequest = activeRequest,
                    selectedGuard = selectedGuard,
                    matchedGuardProfile = matchedGuardProfile,
                    distanceDisplay = distanceDisplay,
                    onViewGuardProfile = onViewGuardProfile,
                    onNavigateToRadar = onNavigateToRadar,
                    onNewRequest = {
                        onDispatchGuardRequested?.invoke(
                            selectedGuard?.guardName ?: activeRequest?.guardName ?: "Request Protection"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun Tactile3DMapActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    shape: Shape,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(48.dp)
            .threeDTactilePush(isPressed = isPressed, depth = 3.dp)
            .shadow(elevation = if (isPressed) 1.dp else 5.dp, shape = shape)
            .clip(shape)
            .background(containerColor)
            .border(1.2.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
