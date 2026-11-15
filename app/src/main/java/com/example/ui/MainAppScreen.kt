package com.example.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.location.PanicLocationProvider
import com.example.data.model.SecurityGuardProfile
import com.example.data.repository.SecurityRepository
import com.example.service.DispatchNotificationHelper
import com.example.service.EmergencyNotificationHelper
import com.example.ui.auth.signOutUser
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.ui.components.AnimatedSequreLogo
import com.example.ui.components.FloatingPanicButton
import com.example.ui.components.PanicConfirmationDialog
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.SosHoldProgressOverlay
import com.example.ui.components.SosTransmissionStatusIndicator
import com.example.ui.dispatch.RequestServiceScreen
import com.example.ui.guard.GuardDetailScreen
import com.example.ui.history.MyBookingsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.TacticalGreen
import com.example.ui.tracking.ActiveTrackingScreen
import com.example.ui.tracking.MapScreen
import com.example.ui.tracking.RadarTelemetryCalculator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    currentUserId: String,
    onSignOutComplete: () -> Unit,
    openTrackingOnLaunch: Boolean = false,
    deepLinkRequestId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val repository = remember { SecurityRepository(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = MainViewModel.Factory(repository, currentUserId)
    )

    var currentTab by remember { mutableIntStateOf(if (openTrackingOnLaunch) 1 else 0) } // 0: Dispatch, 1: Live Map, 2: Radar, 3: History, 4: Account
    var selectedGuardProfile by remember { mutableStateOf<SecurityGuardProfile?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(PanicLocationProvider.hasLocationPermission(context))
    }
    var isPanicLocating by remember { mutableStateOf(false) }
    var panicNotificationSent by remember { mutableStateOf(false) }
    var smsComposerLaunched by remember { mutableStateOf(false) }
    var isHoldingSos by remember { mutableStateOf(false) }
    var sosHoldProgress by remember { mutableStateOf(0f) }

    // BackHandler: clears guard detail or returns to Dispatch tab
    BackHandler(enabled = selectedGuardProfile != null || currentTab != 0) {
        if (selectedGuardProfile != null) {
            selectedGuardProfile = null
        } else {
            currentTab = 0
        }
    }

    val catalogUiState by viewModel.catalogState.collectAsStateWithLifecycle()
    val deployments by viewModel.deploymentsState.collectAsStateWithLifecycle()
    val requestsUiState by viewModel.requestsState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
    val fcmRegistrationActive by viewModel.fcmRegistrationActive.collectAsStateWithLifecycle()
    val realtimeStatusAlert by viewModel.realtimeStatusAlert.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val lastPanicReceipt by viewModel.lastPanicReceipt.collectAsStateWithLifecycle()
    val isSosTransmittedToServer by viewModel.isSosTransmittedToServer.collectAsStateWithLifecycle()
    var preselectedBookingLocation by remember { mutableStateOf<String?>(null) }
    var hasNotificationPermission by remember {
        mutableStateOf(DispatchNotificationHelper.hasNotificationPermission(context))
    }
    val isPanicBusy = isPanicLocating || isSubmitting

    val snackbarHostState = remember { SnackbarHostState() }

    val profileNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted || DispatchNotificationHelper.hasNotificationPermission(context)
        if (hasNotificationPermission) {
            viewModel.registerFcmTokenAndTopics()
        }
    }

    fun executePanicDispatch() {
        coroutineScope.launch {
            isPanicLocating = true
            smsComposerLaunched = false
            EmergencyNotificationHelper.triggerPanicHaptics(context)
            val gpsFix = PanicLocationProvider.acquireCurrentGpsLocation(
                context = context,
                fallbackLocationName = userProfile?.resolvedDefaultAddress ?: userProfile?.defaultLocation
            )
            isPanicLocating = false
            viewModel.triggerPanicDispatch(gpsFix) { receipt ->
                panicNotificationSent = EmergencyNotificationHelper.sendPanicDispatchNotification(
                    context = context,
                    requestId = receipt.requestId,
                    locationLabel = receipt.locationLabel,
                    latitude = receipt.latitude,
                    longitude = receipt.longitude,
                    isLiveGps = receipt.isLiveGps,
                    emergencyContactSummary = receipt.emergencyContactSummary
                )
                if (!receipt.emergencyContactPhone.isNullOrBlank()) {
                    smsComposerLaunched = EmergencyNotificationHelper.launchEmergencyContactSms(
                        context = context,
                        phone = receipt.emergencyContactPhone,
                        message = receipt.emergencyAlertMessage
                    )
                }
                currentTab = 1
            }
        }
    }

    val panicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
            hasNotificationPermission = true
            viewModel.registerFcmTokenAndTopics()
        }
        executePanicDispatch()
    }

    val onPanicTriggered: () -> Unit = {
        if (!isPanicBusy) {
            val missingPermissions = buildList {
                if (!PanicLocationProvider.hasLocationPermission(context)) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (missingPermissions.isEmpty()) {
                hasLocationPermission = true
                executePanicDispatch()
            } else {
                panicPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    // Request notification permission only when user submits a booking and needs dispatch alerts
    var pendingDispatchRequest by remember { mutableStateOf<com.example.data.model.SecurityRequest?>(null) }
    val bookingNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasNotificationPermission = true
            viewModel.registerFcmTokenAndTopics()
        }
        pendingDispatchRequest?.let { req ->
            pendingDispatchRequest = null
            viewModel.submitSecurityRequest(req) { currentTab = 1 }
        }
    }

    val submitBookingRequest: (com.example.data.model.SecurityRequest) -> Unit = { req ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDispatchRequest = req
            bookingNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.registerFcmTokenAndTopics()
            viewModel.submitSecurityRequest(req) { currentTab = 1 }
        }
    }

    // Sync FCM device token & topics on authenticated session start
    LaunchedEffect(currentUserId) {
        viewModel.registerFcmTokenAndTopics()
    }

    // Handle FCM push notification deep-link into live tracking
    LaunchedEffect(openTrackingOnLaunch, deepLinkRequestId) {
        if (openTrackingOnLaunch || !deepLinkRequestId.isNullOrBlank()) {
            deepLinkRequestId?.takeIf { it.isNotBlank() }?.let { id ->
                viewModel.selectRequestById(id)
            }
            currentTab = 1
            onDeepLinkConsumed()
        }
    }

    // Foreground FCM push arrival bridge
    LaunchedEffect(Unit) {
        DispatchNotificationHelper.liveStatusEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = "${event.title} — ${event.body}",
                actionLabel = "Track Live",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (event.requestId.isNotBlank()) {
                    viewModel.selectRequestById(event.requestId)
                }
                currentTab = 1
            }
        }
    }

    // Real-time Firestore status change alert (service started / guard arrived on-site)
    LaunchedEffect(realtimeStatusAlert) {
        val alert = realtimeStatusAlert ?: return@LaunchedEffect
        DispatchNotificationHelper.showStatusUpdateNotification(
            context = context,
            requestId = alert.requestId,
            status = alert.status,
            title = alert.title,
            body = alert.body
        )
        val result = snackbarHostState.showSnackbar(
            message = "${alert.title} — ${alert.body}",
            actionLabel = "Track Live",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed && alert.requestId.isNotBlank()) {
            viewModel.selectRequestById(alert.requestId)
            currentTab = 1
        }
        viewModel.dismissRealtimeStatusAlert()
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val requestList = (requestsUiState as? UiState.Success)?.data ?: emptyList()
    val activeRequest = viewModel.resolveActiveRequest(requestList)
    val activeDeployment = viewModel.resolveDeploymentForRequest(activeRequest, deployments)
    val hasActiveSosOnServer = isSosTransmittedToServer || (activeRequest?.isActive == true && activeRequest.isEmergency)

    val infiniteTransition = rememberInfiniteTransition(label = "bg_gradient_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_progress"
    )

    val animatedColor1 = lerp(Color(0xFFF8FAFC), Color(0xFFF1F5F9), animProgress)
    val animatedColor2 = lerp(Color(0xFFFFFFFF), Color(0xFFE2E8F0), animProgress)
    val animatedBackgroundBrush = Brush.verticalGradient(
        colors = listOf(animatedColor1, animatedColor2, Color(0xFFF8FAFC))
    )

    if (selectedGuardProfile != null) {
        val guard = selectedGuardProfile!!
        GuardDetailScreen(
            guard = guard,
            onBackClicked = {
                selectedGuardProfile = null
            },
            onRequestServiceClicked = {
                selectedGuardProfile = null
                currentTab = 0
            },
            onCallGuardClicked = {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${guard.phone}"))
                    context.startActivity(dialIntent)
                } catch (_: ActivityNotFoundException) {
                    // Safe fallback on tablets/devices without dialer app
                }
            }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundBrush)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AnimatedSequreLogo(
                                    size = 38.dp,
                                    showOrbitalRing = true,
                                    showStripedRim = true,
                                    animated = false,
                                    modifier = Modifier.testTag("topbar_animated_logo")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                Text(
                                    text = "SeQure",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Security Guard Dispatch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectedGuardProfile != null || currentTab != 0) {
                            IconButton(
                                onClick = {
                                    if (selectedGuardProfile != null) {
                                        selectedGuardProfile = null
                                    } else {
                                        currentTab = 0
                                    }
                                },
                                modifier = Modifier.testTag("top_bar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back Navigation",
                                    tint = Color(0xFF0F172A)
                                )
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            if (activeRequest != null && activeRequest.isActive && !activeRequest.isEmergency) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TacticalGreen.copy(alpha = 0.15f))
                                        .border(0.8.dp, TacticalGreen, RoundedCornerShape(6.dp))
                                        .clickable { currentTab = 1 }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(TacticalGreen)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = RadarTelemetryCalculator.formatStatusLabel(activeRequest.status),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TacticalGreen
                                        )
                                    }
                                }
                            }

                            SosTransmissionStatusIndicator(
                                isTransmitted = hasActiveSosOnServer,
                                onClick = {
                                    if (hasActiveSosOnServer || activeRequest?.isActive == true) {
                                        currentTab = 1
                                    }
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                    PurpleStripedBanner(
                        height = 4.dp,
                        stripeWidthDp = 8f,
                        gapWidthDp = 8f
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        PurpleStripedBanner(
                            height = 3.dp,
                            stripeWidthDp = 8f,
                            gapWidthDp = 8f
                        )
                        NavigationBar(
                            containerColor = Color.White
                        ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Dispatch",
                                    tint = if (currentTab == 0) SecurityBlueLight else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = "Dispatch",
                                    color = if (currentTab == 0) SecurityBlueLight else Color(0xFF64748B),
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PurpleSurfaceTint
                            ),
                            modifier = Modifier.testTag("nav_dispatch")
                        )

                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Maps Track",
                                    tint = if (currentTab == 1) SecurityBlueLight else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = "Live Map",
                                    color = if (currentTab == 1) SecurityBlueLight else Color(0xFF64748B),
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PurpleSurfaceTint
                            ),
                            modifier = Modifier.testTag("nav_live_map")
                        )

                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "Tracking",
                                    tint = if (currentTab == 2) SecurityBlueLight else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = "Radar",
                                    color = if (currentTab == 2) SecurityBlueLight else Color(0xFF64748B),
                                    fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PurpleSurfaceTint
                            ),
                            modifier = Modifier.testTag("nav_tracking")
                        )

                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = "My Bookings",
                                    tint = if (currentTab == 3) SecurityBlueLight else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = "Bookings",
                                    color = if (currentTab == 3) SecurityBlueLight else Color(0xFF64748B),
                                    fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PurpleSurfaceTint
                            ),
                            modifier = Modifier.testTag("nav_history")
                        )

                        NavigationBarItem(
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = if (currentTab == 4) SecurityBlueLight else Color(0xFF64748B)
                                )
                            },
                            label = {
                                Text(
                                    text = "Account",
                                    color = if (currentTab == 4) SecurityBlueLight else Color(0xFF64748B),
                                    fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = PurpleSurfaceTint
                            ),
                            modifier = Modifier.testTag("nav_profile")
                        )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingPanicButton(
                    isProcessing = isPanicBusy,
                    isSosTransmitted = hasActiveSosOnServer,
                    onClick = onPanicTriggered,
                    onHoldStateChanged = { holding, progress ->
                        isHoldingSos = holding
                        sosHoldProgress = progress
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // NavigationRail for Wide Screens (Tablets / Foldables / ChromeOS)
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(0.8.dp, Color(0xFFE2E8F0))
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.Shield, contentDescription = "Dispatch", tint = if (currentTab == 0) SecurityBlueLight else Color(0xFF64748B)) },
                            label = { Text("Dispatch", color = if (currentTab == 0) SecurityBlueLight else Color(0xFF64748B)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PurpleSurfaceTint),
                            modifier = Modifier.testTag("rail_nav_dispatch")
                        )
                        NavigationRailItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.Map, contentDescription = "Live Map", tint = if (currentTab == 1) SecurityBlueLight else Color(0xFF64748B)) },
                            label = { Text("Live Map", color = if (currentTab == 1) SecurityBlueLight else Color(0xFF64748B)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PurpleSurfaceTint),
                            modifier = Modifier.testTag("rail_nav_live_map")
                        )
                        NavigationRailItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.GpsFixed, contentDescription = "Radar", tint = if (currentTab == 2) SecurityBlueLight else Color(0xFF64748B)) },
                            label = { Text("Radar", color = if (currentTab == 2) SecurityBlueLight else Color(0xFF64748B)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PurpleSurfaceTint),
                            modifier = Modifier.testTag("rail_nav_tracking")
                        )
                        NavigationRailItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "My Bookings", tint = if (currentTab == 3) SecurityBlueLight else Color(0xFF64748B)) },
                            label = { Text("Bookings", color = if (currentTab == 3) SecurityBlueLight else Color(0xFF64748B)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PurpleSurfaceTint),
                            modifier = Modifier.testTag("rail_nav_history")
                        )
                        NavigationRailItem(
                            selected = currentTab == 4,
                            onClick = { currentTab = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", tint = if (currentTab == 4) SecurityBlueLight else Color(0xFF64748B)) },
                            label = { Text("Account", color = if (currentTab == 4) SecurityBlueLight else Color(0xFF64748B)) },
                            colors = NavigationRailItemDefaults.colors(indicatorColor = PurpleSurfaceTint),
                            modifier = Modifier.testTag("rail_nav_profile")
                        )
                    }
                }

                // Centered Container with Responsive Max Width Constraint
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                    ) {
                        when (currentTab) {
                            0 -> RequestServiceScreen(
                                    catalogState = catalogUiState,
                                    onRetryCatalog = { viewModel.retryLoadCatalog() },
                                    userProfile = userProfile,
                                    preselectedLocation = preselectedBookingLocation,
                                    onConsumePreselectedLocation = { preselectedBookingLocation = null },
                                    onNavigateToProfile = { currentTab = 4 },
                                    onRequestSubmitted = submitBookingRequest,
                                    isSubmitting = isSubmitting,
                                    hasLocationPermission = hasLocationPermission
                                )

                                1 -> MapScreen(
                                    nearbyGuards = deployments,
                                    activeRequest = activeRequest,
                                    activeDeployment = activeDeployment,
                                    onNavigateToRadar = {
                                        currentTab = 2
                                    },
                                    onDispatchGuardRequested = { _ ->
                                        currentTab = 0
                                    },
                                    onViewGuardProfile = { guard ->
                                        selectedGuardProfile = guard
                                    },
                                    onSubmitFeedback = { reqId, rating, comment ->
                                        viewModel.submitFeedback(reqId, rating, comment)
                                    }
                                )

                                2 -> ActiveTrackingScreen(
                                    activeRequest = activeRequest,
                                    activeDeployment = activeDeployment,
                                    onCancelRequest = { req ->
                                        viewModel.cancelSecurityRequest(req)
                                    },
                                    onNavigateToBooking = {
                                        currentTab = 0
                                    },
                                    onNavigateToLiveMap = {
                                        currentTab = 1
                                    },
                                    onViewGuardProfile = { guard ->
                                        selectedGuardProfile = guard
                                    },
                                    onSubmitFeedback = { reqId, rating, comment ->
                                        viewModel.submitFeedback(reqId, rating, comment)
                                    }
                                )

                                3 -> MyBookingsScreen(
                                    requestsState = requestsUiState,
                                    onSelectRequest = { req ->
                                        viewModel.selectActiveRequest(req)
                                        currentTab = 1
                                    },
                                    onCancelRequest = { req ->
                                        viewModel.cancelSecurityRequest(req)
                                    },
                                    onRetry = {
                                        viewModel.retryLoadBookings()
                                    },
                                    onNavigateToBooking = {
                                        currentTab = 0
                                    }
                                )

                                4 -> ProfileScreen(
                                    userProfile = userProfile,
                                    isSaving = isSavingProfile,
                                    hasNotificationPermission = hasNotificationPermission,
                                    isFcmRegistered = fcmRegistrationActive,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            profileNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onSaveProfile = { name, phone, emName, emPhone, emRelation, homeAddr, businessAddr, preferredType, onDone ->
                                        viewModel.saveProfile(
                                            name = name,
                                            phone = phone,
                                            emergencyContactName = emName,
                                            emergencyContactPhone = emPhone,
                                            emergencyContactRelation = emRelation,
                                            homeAddress = homeAddr,
                                            businessAddress = businessAddr,
                                            preferredAddressType = preferredType,
                                            onComplete = onDone
                                        )
                                    },
                                    onQuickBookAddress = { chosenAddress ->
                                        preselectedBookingLocation = chosenAddress
                                        currentTab = 0
                                    },
                                    onSignOutClicked = {
                                        signOutUser(credentialManager, coroutineScope) {
                                            onSignOutComplete()
                                        }
                                    }
                                )
                        }
                    }
                }
            }
        }

        SosHoldProgressOverlay(
            visible = isHoldingSos || isPanicBusy,
            isProcessing = isPanicBusy,
            progress = sosHoldProgress,
            emergencyContactSummary = userProfile?.effectiveEmergencyContactSummary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )
    }

    lastPanicReceipt?.let { receipt ->
        PanicConfirmationDialog(
            receipt = receipt,
            notificationSent = panicNotificationSent,
            smsComposerLaunched = smsComposerLaunched,
            onSendEmergencySms = {
                val phone = receipt.emergencyContactPhone
                if (!phone.isNullOrBlank()) {
                    val launched = EmergencyNotificationHelper.launchEmergencyContactSms(
                        context = context,
                        phone = phone,
                        message = receipt.emergencyAlertMessage
                    )
                    if (launched) {
                        smsComposerLaunched = true
                    } else {
                        EmergencyNotificationHelper.launchEmergencyShareSheet(
                            context = context,
                            message = receipt.emergencyAlertMessage
                        )
                    }
                }
            },
            onShareEmergencyLocation = {
                EmergencyNotificationHelper.launchEmergencyShareSheet(
                    context = context,
                    message = receipt.emergencyAlertMessage
                )
            },
            onConfigureEmergencyContact = {
                viewModel.dismissPanicReceipt()
                currentTab = 4
            },
            onViewTracking = {
                viewModel.dismissPanicReceipt()
                currentTab = 1
            },
            onDismiss = {
                viewModel.dismissPanicReceipt()
            }
        )
    }
}
