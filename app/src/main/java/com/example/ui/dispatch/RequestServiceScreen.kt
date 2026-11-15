package com.example.ui.dispatch

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import com.example.data.location.PanicLocationProvider
import com.example.data.model.CommandCenterCatalog
import com.example.data.model.CommandCenterService
import com.example.data.model.SecurityRequest
import com.example.data.model.UserProfile
import com.example.ui.UiState
import com.example.ui.components.ResponsiveActionButton
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NeutralBorder
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
fun RequestServiceScreen(
    catalogState: UiState<CommandCenterCatalog>,
    onRetryCatalog: () -> Unit,
    onRequestSubmitted: (SecurityRequest) -> Unit,
    isSubmitting: Boolean = false,
    hasLocationPermission: Boolean = false,
    userProfile: UserProfile? = null,
    preselectedLocation: String? = null,
    onConsumePreselectedLocation: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    when (catalogState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("catalog_loading"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = SecurityBlueLight)
                    Text(
                        text = "Syncing live services & pricing from Command Center...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("catalog_error"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Command Center Connection Error",
                            tint = EmergencyRed,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Command Center Sync Failed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeutralTextPrimary
                        )
                        Text(
                            text = catalogState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        ResponsiveActionButton(
                            text = "Retry Connection",
                            icon = Icons.Default.Refresh,
                            onClick = onRetryCatalog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catalog_retry_button")
                        )
                    }
                }
            }
            return
        }

        is UiState.Success -> {
            val catalog = catalogState.data
            if (catalog.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("catalog_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SecurityCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "No Published Services",
                                tint = SecurityBlueLight,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "Awaiting Command Center Catalog",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeutralTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "No active services or pricing packages have been published by the Command Center web app yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            ResponsiveActionButton(
                                text = "Refresh Catalog",
                                icon = Icons.Default.Refresh,
                                onClick = onRetryCatalog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("catalog_refresh_button")
                            )
                        }
                    }
                }
                return
            }

            var selectedTierCode by remember(catalog.tiers) {
                mutableStateOf<String?>(catalog.tiers.firstOrNull()?.code)
            }
            val visibleServices = remember(catalog, selectedTierCode) {
                val filtered = catalog.servicesForTier(selectedTierCode)
                if (filtered.isEmpty() && selectedTierCode != null) catalog.services else filtered
            }
            var selectedService by remember(catalog.services) {
                mutableStateOf<CommandCenterService?>(catalog.services.firstOrNull())
            }
            LaunchedEffect(visibleServices) {
                val current = selectedService
                val stillPresent = current != null && visibleServices.any {
                    it.id == current.id && it.code == current.code
                }
                if (!stillPresent) {
                    selectedService = visibleServices.firstOrNull()
                } else {
                    selectedService = visibleServices.firstOrNull {
                        it.id == current?.id && it.code == current.code
                    }
                }
            }

            val currentService = selectedService ?: catalog.services.first()
            var selectedTab by remember { mutableIntStateOf(0) } // 0: Immediate, 1: Scheduled

            val savedHome = userProfile?.homeAddress?.takeIf { it.isNotBlank() }
            val savedBusiness = userProfile?.businessAddress?.takeIf { it.isNotBlank() }
            val resolvedDefault = userProfile?.resolvedDefaultAddress

            var locationInput by remember(resolvedDefault) {
                mutableStateOf(resolvedDefault.orEmpty())
            }
            LaunchedEffect(preselectedLocation) {
                if (!preselectedLocation.isNullOrBlank()) {
                    locationInput = preselectedLocation
                    onConsumePreselectedLocation()
                }
            }
            var capturedLat by remember { mutableDoubleStateOf(PanicLocationProvider.UNSET_LAT) }
            var capturedLng by remember { mutableDoubleStateOf(PanicLocationProvider.UNSET_LNG) }
            var isLocatingField by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (locationInput.isBlank() && PanicLocationProvider.hasLocationPermission(context)) {
                    isLocatingField = true
                    val fix = PanicLocationProvider.acquireCurrentGpsLocation(context, null)
                    if (fix.isLiveGps) {
                        locationInput = fix.addressLabel
                        capturedLat = fix.latitude
                        capturedLng = fix.longitude
                    }
                    isLocatingField = false
                }
            }

            var selectedDateUtcMillis by remember {
                mutableLongStateOf(ScheduleDateTimeFormatter.tomorrowUtcMidnightMillis())
            }
            var selectedStartHour by remember { mutableIntStateOf(9) }
            var selectedStartMinute by remember { mutableIntStateOf(0) }
            var durationHours by remember { mutableLongStateOf(4L) }
            var notesInput by remember { mutableStateOf("") }

            val locateFieldLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (granted) {
                    coroutineScope.launch {
                        isLocatingField = true
                        val fix = PanicLocationProvider.acquireCurrentGpsLocation(context, locationInput)
                        if (fix.isLiveGps) {
                            locationInput = fix.addressLabel
                            capturedLat = fix.latitude
                            capturedLng = fix.longitude
                        }
                        isLocatingField = false
                    }
                }
            }

            val activeTier = catalog.findTierForService(currentService)
                ?: catalog.tiers.firstOrNull { it.code.equals(selectedTierCode, ignoreCase = true) }
            val activeTierTitle = activeTier?.title ?: currentService.resolvedTierTitle
            val formattedSchedule = ScheduleDateTimeFormatter.formatFullStartDateTime(
                selectedDateUtcMillis,
                selectedStartHour,
                selectedStartMinute
            )
            val cleanLocation = locationInput.trim()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .testTag("request_service_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STEP 1: SECURITY TIER SELECTOR
                item {
                    SecurityTierSelector(
                        tiers = catalog.tiers,
                        totalServiceCount = catalog.services.size,
                        selectedTierCode = selectedTierCode,
                        onTierSelected = { tier ->
                            selectedTierCode = tier?.code
                            val matching = catalog.servicesForTier(tier?.code)
                            if (matching.isNotEmpty() &&
                                matching.none { it.id == currentService.id && it.code == currentService.code }
                            ) {
                                selectedService = matching.first()
                            }
                        }
                    )
                }

                // STEP 2: SECURITY SERVICES GRID (live from Command Center)
                item {
                    SecurityServiceGrid(
                        services = visibleServices,
                        selectedService = currentService,
                        selectedTierTitle = activeTier?.title,
                        onServiceSelected = { service ->
                            selectedService = service
                            if (selectedTierCode != null) {
                                selectedTierCode = service.tierCode
                            }
                        }
                    )
                }

                // STEP 3: DISPATCH MODE TABS (On-Demand vs Schedule Start)
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = SecurityBlueLight,
                        indicator = { tabPositions ->
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = SecurityBlueLight,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SecurityCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = if (selectedTab == 0) SecurityBlueLight else NeutralTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "On-Demand (Now)",
                                        fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == 0) SecurityBlueLight else NeutralTextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_ondemand")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (selectedTab == 1) SecurityBlueLight else NeutralTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Schedule Start",
                                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == 1) SecurityBlueLight else NeutralTextSecondary
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_schedule")
                        )
                    }
                }

                // DEPLOYMENT LOCATION & DATE-TIME PICKER CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Deployment Location",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SecurityBluePrimary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (savedHome != null) {
                                    item {
                                        val isHomeSelected = locationInput == savedHome
                                        FilterChip(
                                            selected = isHomeSelected,
                                            onClick = { locationInput = savedHome },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Home,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("Home") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFECFDF5),
                                                selectedLabelColor = TacticalGreen,
                                                selectedLeadingIconColor = TacticalGreen,
                                                containerColor = Color.White,
                                                labelColor = NeutralTextSecondary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isHomeSelected,
                                                borderColor = NeutralBorder,
                                                selectedBorderColor = TacticalGreen
                                            ),
                                            modifier = Modifier.testTag("quick_fill_home_address")
                                        )
                                    }
                                }

                                if (savedBusiness != null) {
                                    item {
                                        val isBusinessSelected = locationInput == savedBusiness
                                        FilterChip(
                                            selected = isBusinessSelected,
                                            onClick = { locationInput = savedBusiness },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Apartment,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("Business") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PurpleSurfaceTint,
                                                selectedLabelColor = SecurityBlueLight,
                                                selectedLeadingIconColor = SecurityBlueLight,
                                                containerColor = Color.White,
                                                labelColor = NeutralTextSecondary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isBusinessSelected,
                                                borderColor = NeutralBorder,
                                                selectedBorderColor = SecurityBlueLight
                                            ),
                                            modifier = Modifier.testTag("quick_fill_business_address")
                                        )
                                    }
                                }

                                if (savedHome == null && savedBusiness == null) {
                                    item {
                                        FilterChip(
                                            selected = false,
                                            onClick = onNavigateToProfile,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Home,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("Save Home / Business Address") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color(0xFFF8FAFC),
                                                labelColor = SecurityBlueLight,
                                                iconColor = SecurityBlueLight
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = false,
                                                borderColor = SecurityBlueLight.copy(alpha = 0.45f)
                                            ),
                                            modifier = Modifier.testTag("open_profile_addresses_chip")
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = locationInput,
                                onValueChange = { locationInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("location_input"),
                                placeholder = { Text("Enter street address or tap GPS icon") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = SecurityBlueLight
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (PanicLocationProvider.hasLocationPermission(context)) {
                                                coroutineScope.launch {
                                                    isLocatingField = true
                                                    val fix = PanicLocationProvider.acquireCurrentGpsLocation(
                                                        context,
                                                        locationInput
                                                    )
                                                    if (fix.isLiveGps) {
                                                        locationInput = fix.addressLabel
                                                        capturedLat = fix.latitude
                                                        capturedLng = fix.longitude
                                                    }
                                                    isLocatingField = false
                                                }
                                            } else {
                                                locateFieldLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("locate_me_button")
                                    ) {
                                        if (isLocatingField) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = TacticalGreen
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "Use Current GPS Location",
                                                tint = if (hasLocationPermission) TacticalGreen else SecurityBlueLight
                                            )
                                        }
                                    }
                                },
                                label = { Text("Site Address or GPS Coordinates") },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = NeutralTextPrimary,
                                    unfocusedTextColor = NeutralTextPrimary,
                                    focusedLabelColor = SecurityBlueLight,
                                    unfocusedLabelColor = NeutralTextSecondary,
                                    focusedBorderColor = SecurityBlueLight,
                                    unfocusedBorderColor = NeutralBorder,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true
                            )

                            if (cleanLocation.isBlank()) {
                                Text(
                                    text = "Site address or live GPS fix is required to dispatch.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmergencyRed
                                )
                            }

                            AnimatedVisibility(visible = selectedTab == 1) {
                                ScheduledDateTimeSection(
                                    selectedDateUtcMillis = selectedDateUtcMillis,
                                    selectedHour = selectedStartHour,
                                    selectedMinute = selectedStartMinute,
                                    durationHours = durationHours,
                                    onDateSelected = { selectedDateUtcMillis = it },
                                    onTimeSelected = { hour, minute ->
                                        selectedStartHour = hour
                                        selectedStartMinute = minute
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notes_input"),
                                label = { Text("Special Instructions (Entry codes, requirements)") },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = NeutralTextPrimary,
                                    unfocusedTextColor = NeutralTextPrimary,
                                    focusedLabelColor = SecurityBlueLight,
                                    unfocusedLabelColor = NeutralTextSecondary,
                                    focusedBorderColor = SecurityBlueLight,
                                    unfocusedBorderColor = NeutralBorder,
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color.White
                                ),
                                maxLines = 3
                            )
                        }
                    }
                }

                // SHIFT DURATION CARD
                item {
                    ShiftDurationCard(
                        selectedService = currentService,
                        durationHours = durationHours,
                        onDurationChanged = { durationHours = it }
                    )
                }

                // BOOKING SUMMARY CARD
                item {
                    BookingSummaryCard(
                        tierTitle = activeTierTitle,
                        service = currentService,
                        startTimeLabel = if (selectedTab == 1) formattedSchedule else "Immediate (On-Demand)",
                        durationHours = durationHours,
                        location = cleanLocation
                    )
                }

                // SUBMIT BOOKING CTA BUTTON
                item {
                    ResponsiveActionButton(
                        text = if (selectedTab == 0) {
                            "Request ${currentService.title} ($durationHours ${if (durationHours == 1L) "hr" else "hrs"})"
                        } else {
                            "Confirm Scheduled Booking ($durationHours ${if (durationHours == 1L) "hr" else "hrs"})"
                        },
                        icon = if (selectedTab == 0) Icons.Default.FlashOn else Icons.Default.CalendarMonth,
                        containerColor = if (selectedTab == 0) SecurityBlueLight else SecurityBluePrimary,
                        isLoading = isSubmitting,
                        enabled = cleanLocation.isNotBlank() && !isSubmitting,
                        minHeight = 54.dp,
                        onClick = {
                            val isScheduled = selectedTab == 1
                            val targetTime = if (isScheduled) formattedSchedule else "Immediate (On-Demand)"
                            val req = SecurityRequest(
                                securityTier = activeTierTitle,
                                serviceType = currentService.code,
                                serviceName = currentService.title,
                                status = "REQUESTED",
                                isEmergency = false,
                                locationName = cleanLocation,
                                latitude = capturedLat,
                                longitude = capturedLng,
                                targetDateTime = targetTime,
                                isScheduled = isScheduled,
                                durationHours = durationHours,
                                hourlyRate = currentService.hourlyRate,
                                priceLabel = currentService.priceLabel,
                                estimatedTotalLabel = currentService.estimateTotalLabel(durationHours),
                                notes = notesInput.ifBlank { null }
                            )
                            onRequestSubmitted(req)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_request_button")
                    )
                    // Extra bottom clearance so persistent FloatingPanicButton does not overlap CTA
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }
}
