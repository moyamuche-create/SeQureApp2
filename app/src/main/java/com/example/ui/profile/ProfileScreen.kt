package com.example.ui.profile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.PanicLocationProvider
import com.example.data.model.UserProfile
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.ResponsiveActionButton
import com.example.ui.components.ResponsiveOutlinedButton
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    onSaveProfile: (
        name: String,
        phone: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        emergencyContactRelation: String,
        homeAddress: String,
        businessAddress: String,
        preferredAddressType: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onQuickBookAddress: (String) -> Unit = {},
    onSignOutClicked: () -> Unit,
    isSaving: Boolean = false,
    hasNotificationPermission: Boolean = true,
    isFcmRegistered: Boolean = false,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    var nameInput by remember(userProfile) {
        mutableStateOf(userProfile?.displayName?.ifBlank { null } ?: currentUser?.displayName ?: "Verified Client")
    }
    var phoneInput by remember(userProfile) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var emergencyNameInput by remember(userProfile) {
        mutableStateOf(
            userProfile?.emergencyContactName
                ?: userProfile?.emergencyContact?.substringBefore("(")?.substringBefore("•")?.trim().orEmpty()
        )
    }
    var emergencyPhoneInput by remember(userProfile) {
        mutableStateOf(
            userProfile?.emergencyContactPhone
                ?: userProfile?.emergencyContact?.substringAfter("•", "")?.trim().orEmpty()
        )
    }
    var emergencyRelationInput by remember(userProfile) {
        mutableStateOf(userProfile?.emergencyContactRelation ?: "Family")
    }
    var homeAddressInput by remember(userProfile) {
        mutableStateOf(
            userProfile?.homeAddress
                ?: if (userProfile?.preferredAddressType != UserProfile.ADDRESS_TYPE_BUSINESS) {
                    userProfile?.defaultLocation.orEmpty()
                } else {
                    ""
                }
        )
    }
    var businessAddressInput by remember(userProfile) {
        mutableStateOf(
            userProfile?.businessAddress
                ?: if (userProfile?.preferredAddressType == UserProfile.ADDRESS_TYPE_BUSINESS) {
                    userProfile.defaultLocation.orEmpty()
                } else {
                    ""
                }
        )
    }
    var preferredAddressType by remember(userProfile) {
        mutableStateOf(userProfile?.preferredAddressType ?: UserProfile.ADDRESS_TYPE_HOME)
    }

    var saveSuccessMsg by remember { mutableStateOf(false) }
    var locatingTarget by remember { mutableStateOf<String?>(null) } // "HOME" or "BUSINESS"

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val target = locatingTarget
        if (granted && target != null) {
            coroutineScope.launch {
                val fix = PanicLocationProvider.acquireCurrentGpsLocation(context, null)
                if (fix.isLiveGps) {
                    if (target == UserProfile.ADDRESS_TYPE_HOME) {
                        homeAddressInput = fix.addressLabel
                    } else {
                        businessAddressInput = fix.addressLabel
                    }
                    saveSuccessMsg = false
                }
                locatingTarget = null
            }
        } else {
            locatingTarget = null
        }
    }

    fun fillWithCurrentGps(targetType: String) {
        locatingTarget = targetType
        if (PanicLocationProvider.hasLocationPermission(context)) {
            coroutineScope.launch {
                val fix = PanicLocationProvider.acquireCurrentGpsLocation(context, null)
                if (fix.isLiveGps) {
                    if (targetType == UserProfile.ADDRESS_TYPE_HOME) {
                        homeAddressInput = fix.addressLabel
                    } else {
                        businessAddressInput = fix.addressLabel
                    }
                    saveSuccessMsg = false
                }
                locatingTarget = null
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = NeutralTextPrimary,
        unfocusedTextColor = NeutralTextPrimary,
        focusedLabelColor = SecurityBlueLight,
        unfocusedLabelColor = NeutralTextSecondary,
        focusedBorderColor = SecurityBlueLight,
        unfocusedBorderColor = NeutralBorder,
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color.White
    )

    val savedAddressesCount = listOf(homeAddressInput, businessAddressInput).count { it.isNotBlank() }
    val hasEmergencyContact = emergencyNameInput.isNotBlank() || emergencyPhoneInput.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. USER IDENTITY HEADER CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, SecurityCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PurpleStripedBanner(
                    height = 5.dp,
                    stripeWidthDp = 8f,
                    gapWidthDp = 8f
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PurpleSurfaceTint)
                            .border(2.dp, SecurityBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile Avatar",
                            tint = SecurityBlueLight,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = nameInput.ifBlank { "Verified Client" },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NeutralTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = currentUser?.email ?: userProfile?.email ?: "client@sequre.app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeutralTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadgePill(
                            text = if (hasEmergencyContact) "SOS Contact Ready" else "Add SOS Contact",
                            tint = if (hasEmergencyContact) TacticalGreen else GoldAccent,
                            icon = Icons.Default.VerifiedUser
                        )
                        StatusBadgePill(
                            text = "$savedAddressesCount/2 Addresses Saved",
                            tint = SecurityBlueLight,
                            icon = Icons.Default.Home
                        )
                    }
                }
            }
        }

        // 2. PERSONAL CONTACT DETAILS CARD
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
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SecurityBluePrimary
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; saveSuccessMsg = false },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SecurityBlueLight
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    colors = fieldColors,
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it; saveSuccessMsg = false },
                    label = { Text("Primary Phone Number") },
                    placeholder = { Text("+1 (555) 012-3456") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = SecurityBlueLight
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input"),
                    colors = fieldColors,
                    singleLine = true
                )
            }
        }

        // 3. EMERGENCY CONTACT INFORMATION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmergencyRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .testTag("emergency_contact_section")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContactEmergency,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency Contact",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeutralTextPrimary
                    )
                }

                Text(
                    text = "Included automatically with high-priority SOS Alerts and active security dispatches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )

                OutlinedTextField(
                    value = emergencyNameInput,
                    onValueChange = { emergencyNameInput = it; saveSuccessMsg = false },
                    label = { Text("Emergency Contact Name") },
                    placeholder = { Text("e.g., Sarah Carter") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = EmergencyRed
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_emergency_name_input"),
                    colors = fieldColors,
                    singleLine = true
                )

                OutlinedTextField(
                    value = emergencyPhoneInput,
                    onValueChange = { emergencyPhoneInput = it; saveSuccessMsg = false },
                    label = { Text("Emergency Contact Phone") },
                    placeholder = { Text("e.g., +1 (555) 098-7654") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = EmergencyRed
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_emergency_phone_input"),
                    colors = fieldColors,
                    singleLine = true
                )

                Text(
                    text = "Relationship",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeutralTextSecondary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val relationOptions = listOf("Spouse", "Family", "Friend", "Colleague", "Security Mgr")
                    items(relationOptions) { option ->
                        val isSelected = emergencyRelationInput.equals(option, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                emergencyRelationInput = option
                                saveSuccessMsg = false
                            },
                            label = {
                                Text(
                                    text = option,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF2F2),
                                selectedLabelColor = EmergencyRed,
                                containerColor = Color.White,
                                labelColor = NeutralTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = NeutralBorder,
                                selectedBorderColor = EmergencyRed
                            ),
                            modifier = Modifier.testTag("relation_chip_$option")
                        )
                    }
                }
            }
        }

        // 4. SAVED HOME & BUSINESS ADDRESSES CARD (FASTER SERVICE BOOKING)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
                .testTag("saved_addresses_section")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Saved Addresses (Fast Booking)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SecurityBluePrimary
                )

                Text(
                    text = "Save your Home and Business locations for 1-tap address selection when dispatching a guard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )

                // Home Address Field & Controls
                AddressEntryBlock(
                    title = "Home Address",
                    addressValue = homeAddressInput,
                    onAddressChanged = { homeAddressInput = it; saveSuccessMsg = false },
                    isPreferred = preferredAddressType == UserProfile.ADDRESS_TYPE_HOME,
                    onSetPreferred = {
                        preferredAddressType = UserProfile.ADDRESS_TYPE_HOME
                        saveSuccessMsg = false
                    },
                    isLocating = locatingTarget == UserProfile.ADDRESS_TYPE_HOME,
                    onUseCurrentGps = { fillWithCurrentGps(UserProfile.ADDRESS_TYPE_HOME) },
                    onQuickBook = if (homeAddressInput.isNotBlank()) {
                        { onQuickBookAddress(homeAddressInput.trim()) }
                    } else null,
                    icon = Icons.Default.Home,
                    accentColor = TacticalGreen,
                    inputTestTag = "profile_home_address_input",
                    defaultButtonTag = "preferred_home_button",
                    gpsButtonTag = "gps_fill_home_button",
                    quickBookTag = "quick_book_home_button",
                    fieldColors = fieldColors
                )

                // Business Address Field & Controls
                AddressEntryBlock(
                    title = "Business Address",
                    addressValue = businessAddressInput,
                    onAddressChanged = { businessAddressInput = it; saveSuccessMsg = false },
                    isPreferred = preferredAddressType == UserProfile.ADDRESS_TYPE_BUSINESS,
                    onSetPreferred = {
                        preferredAddressType = UserProfile.ADDRESS_TYPE_BUSINESS
                        saveSuccessMsg = false
                    },
                    isLocating = locatingTarget == UserProfile.ADDRESS_TYPE_BUSINESS,
                    onUseCurrentGps = { fillWithCurrentGps(UserProfile.ADDRESS_TYPE_BUSINESS) },
                    onQuickBook = if (businessAddressInput.isNotBlank()) {
                        { onQuickBookAddress(businessAddressInput.trim()) }
                    } else null,
                    icon = Icons.Default.Apartment,
                    accentColor = SecurityBlueLight,
                    inputTestTag = "profile_business_address_input",
                    defaultButtonTag = "preferred_business_button",
                    gpsButtonTag = "gps_fill_business_button",
                    quickBookTag = "quick_book_business_button",
                    fieldColors = fieldColors
                )
            }
        }

        if (saveSuccessMsg) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TacticalGreen.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_saved_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TacticalGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Emergency contact & saved addresses synced for fast dispatch.",
                        color = TacticalGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // SAVE PROFILE BUTTON
        ResponsiveActionButton(
            text = "Save Profile & Addresses",
            icon = Icons.Default.Save,
            isLoading = isSaving,
            minHeight = 52.dp,
            onClick = {
                onSaveProfile(
                    nameInput,
                    phoneInput,
                    emergencyNameInput,
                    emergencyPhoneInput,
                    emergencyRelationInput,
                    homeAddressInput,
                    businessAddressInput,
                    preferredAddressType
                ) { success ->
                    saveSuccessMsg = success
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_profile_button")
        )

        // 5. REAL-TIME PUSH ALERTS (FCM) STATUS CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SecurityCardBorder, RoundedCornerShape(14.dp))
                .testTag("fcm_alerts_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (hasNotificationPermission) TacticalGreen else GoldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "Real-Time Dispatch Alerts (FCM)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NeutralTextPrimary
                        )
                        Text(
                            text = when {
                                !hasNotificationPermission -> "Enable push permissions to receive alerts when your guard starts or arrives."
                                isFcmRegistered -> "Device synced • Instant push notifications active for dispatch & arrival."
                                else -> "Ready • Alerts trigger when a booked guard starts or arrives on-site."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary
                        )
                    }
                }

                if (!hasNotificationPermission) {
                    ResponsiveOutlinedButton(
                        text = "Enable",
                        accentColor = SecurityBlueLight,
                        onClick = onRequestNotificationPermission,
                        minHeight = 48.dp,
                        cornerRadius = 10.dp,
                        modifier = Modifier.testTag("enable_fcm_alerts_button")
                    )
                }
            }
        }

        // SIGN OUT ACTION
        ResponsiveOutlinedButton(
            text = "Sign Out of SeQure",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            accentColor = EmergencyRed,
            onClick = onSignOutClicked,
            minHeight = 50.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_out_button")
        )

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun AddressEntryBlock(
    title: String,
    addressValue: String,
    onAddressChanged: (String) -> Unit,
    isPreferred: Boolean,
    onSetPreferred: () -> Unit,
    isLocating: Boolean,
    onUseCurrentGps: () -> Unit,
    onQuickBook: (() -> Unit)?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    inputTestTag: String,
    defaultButtonTag: String,
    gpsButtonTag: String,
    quickBookTag: String,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = NeutralTextPrimary
                )
            }

            FilterChip(
                selected = isPreferred,
                onClick = onSetPreferred,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = if (isPreferred) "Default" else "Set Default",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.14f),
                    selectedLabelColor = accentColor,
                    selectedLeadingIconColor = accentColor,
                    containerColor = Color.White,
                    labelColor = NeutralTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isPreferred,
                    borderColor = NeutralBorder,
                    selectedBorderColor = accentColor
                ),
                modifier = Modifier.testTag(defaultButtonTag)
            )
        }

        OutlinedTextField(
            value = addressValue,
            onValueChange = onAddressChanged,
            placeholder = { Text("Enter street, city, or suite number") },
            trailingIcon = {
                IconButton(
                    onClick = onUseCurrentGps,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(gpsButtonTag)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use Current GPS for $title",
                            tint = accentColor
                        )
                    }
                }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(inputTestTag),
            colors = fieldColors,
            singleLine = true
        )

        if (onQuickBook != null) {
            ResponsiveOutlinedButton(
                text = "Book Guard at $title",
                accentColor = accentColor,
                onClick = onQuickBook,
                minHeight = 48.dp,
                cornerRadius = 10.dp,
                modifier = Modifier.testTag(quickBookTag)
            )
        }
    }
}

@Composable
private fun StatusBadgePill(
    text: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
