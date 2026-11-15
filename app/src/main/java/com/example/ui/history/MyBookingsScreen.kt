package com.example.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityRequest
import com.example.ui.UiState
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.ResponsiveActionButton
import com.example.ui.components.ResponsiveOutlinedButton
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleBorderTint
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.tracking.StatusBadge

@Composable
fun MyBookingsScreen(
    requestsState: UiState<List<SecurityRequest>>,
    onSelectRequest: (SecurityRequest) -> Unit,
    onCancelRequest: (SecurityRequest) -> Unit,
    onRetry: () -> Unit,
    onNavigateToBooking: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, SCHEDULED, PAST

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("request_history_screen")
    ) {
        when (requestsState) {
            is UiState.Loading -> {
                BookingsHeader(
                    activeCount = 0,
                    pastCount = 0,
                    isSyncing = true
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .testTag("bookings_loading"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = SecurityBlueLight)
                        Text(
                            text = "Fetching your bookings from Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralTextSecondary
                        )
                    }
                }
            }

            is UiState.Error -> {
                BookingsHeader(
                    activeCount = 0,
                    pastCount = 0,
                    isSyncing = false
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("bookings_error"),
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
                                contentDescription = "Bookings Sync Error",
                                tint = EmergencyRed,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Unable to Load Bookings",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeutralTextPrimary
                            )
                            Text(
                                text = requestsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            ResponsiveActionButton(
                                text = "Retry Firestore Sync",
                                icon = Icons.Default.Refresh,
                                onClick = onRetry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bookings_retry_button")
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val requests = requestsState.data
                val activeRequests = requests.filter { it.isActive }
                val pastRequests = requests.filter { it.isCompleted || it.isCancelled }
                val scheduledRequests = requests.filter { it.isScheduled }

                BookingsHeader(
                    activeCount = activeRequests.size,
                    pastCount = pastRequests.size,
                    isSyncing = false
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${requests.size})",
                        "ACTIVE" to "Active (${activeRequests.size})",
                        "SCHEDULED" to "Scheduled (${scheduledRequests.size})",
                        "PAST" to "Past (${pastRequests.size})"
                    )

                    items(filters) { (code, label) ->
                        val isSelected = selectedFilter == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = code },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
                            modifier = Modifier.testTag("filter_chip_$code")
                        )
                    }
                }

                val filteredList = when (selectedFilter) {
                    "ACTIVE" -> activeRequests
                    "SCHEDULED" -> scheduledRequests
                    "PAST" -> pastRequests
                    else -> requests
                }

                if (filteredList.isEmpty()) {
                    EmptyBookingsState(
                        hasAnyBookings = requests.isNotEmpty(),
                        onNavigateToBooking = onNavigateToBooking
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedFilter == "ALL") {
                            if (activeRequests.isNotEmpty()) {
                                item {
                                    BookingsSectionTitle(
                                        title = "Active Requests (${activeRequests.size})",
                                        accentColor = TacticalGreen
                                    )
                                }
                                itemsIndexed(
                                    activeRequests,
                                    key = { index, item -> "active_${index}_${item.id}" }
                                ) { _, request ->
                                    BookingItemCard(
                                        request = request,
                                        onClick = { onSelectRequest(request) },
                                        onCancelClick = { onCancelRequest(request) }
                                    )
                                }
                            }

                            if (pastRequests.isNotEmpty()) {
                                item {
                                    BookingsSectionTitle(
                                        title = "Past Bookings (${pastRequests.size})",
                                        accentColor = NeutralTextSecondary
                                    )
                                }
                                itemsIndexed(
                                    pastRequests,
                                    key = { index, item -> "past_${index}_${item.id}" }
                                ) { _, request ->
                                    BookingItemCard(
                                        request = request,
                                        onClick = { onSelectRequest(request) },
                                        onCancelClick = { onCancelRequest(request) }
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                filteredList,
                                key = { index, item -> "filtered_${index}_${item.id}" }
                            ) { _, request ->
                                BookingItemCard(
                                    request = request,
                                    onClick = { onSelectRequest(request) },
                                    onCancelClick = { onCancelRequest(request) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingsHeader(
    activeCount: Int,
    pastCount: Int,
    isSyncing: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SecurityCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("my_bookings_header")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PurpleStripedBanner(
                height = 5.dp,
                stripeWidthDp = 8f,
                gapWidthDp = 8f
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Bookings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NeutralTextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = TacticalGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSyncing) "Syncing Firestore..." else "Live Firestore sync",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CountPill(label = "$activeCount Active", color = TacticalGreen)
                    CountPill(label = "$pastCount Past", color = SecurityBlueLight)
                }
            }
        }
    }
}

@Composable
private fun CountPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun BookingsSectionTitle(
    title: String,
    accentColor: Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyBookingsState(
    hasAnyBookings: Boolean,
    onNavigateToBooking: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("bookings_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PurpleSurfaceTint)
                    .border(1.dp, PurpleBorderTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                    contentDescription = null,
                    tint = SecurityBlueLight,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = if (hasAnyBookings) "No Matching Bookings" else "No Bookings Yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NeutralTextPrimary
            )

            Text(
                text = if (hasAnyBookings) {
                    "No security bookings match the selected status filter."
                } else {
                    "Your active and past security service bookings will appear here once dispatched."
                },
                style = MaterialTheme.typography.bodySmall,
                color = NeutralTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            ResponsiveActionButton(
                text = "Book Security Service",
                icon = Icons.Default.Shield,
                onClick = onNavigateToBooking,
                modifier = Modifier.testTag("bookings_empty_cta_button")
            )
        }
    }
}

@Composable
private fun BookingItemCard(
    request: SecurityRequest,
    onClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (request.isEmergency) EmergencyRed.copy(alpha = 0.55f) else SecurityCardBorder,
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${request.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (request.isEmergency) EmergencyRed.copy(alpha = 0.12f)
                                else SecurityBlueLight.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (request.isEmergency) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (request.isEmergency) EmergencyRed else SecurityBlueLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = request.serviceName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PurpleSurfaceTint
                            ) {
                                Text(
                                    text = "${request.resolvedTierDisplay} Tier",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecurityBlueLight
                                )
                            }
                            Text(
                                text = if (request.isScheduled) "Scheduled • ${request.durationHours} hrs"
                                else "On-Demand • ${request.durationHours} hrs",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (request.isScheduled) GoldLight else TacticalGreen
                            )
                        }
                    }
                }

                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = NeutralTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = NeutralTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.targetDateTime.ifBlank { "Immediate Dispatch" },
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralTextSecondary
                )
            }

            if (!request.guardName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SecurityBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${request.guardName} (${request.guardBadge ?: "Officer"})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = SecurityBluePrimary
                    )
                }
            }

            if (request.rating != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rated ${request.rating}/5",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NeutralTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.status in listOf("REQUESTED", "DISPATCHED")) {
                    ResponsiveOutlinedButton(
                        text = "Cancel Request",
                        accentColor = EmergencyRed,
                        onClick = onCancelClick,
                        minHeight = 48.dp,
                        cornerRadius = 10.dp,
                        modifier = Modifier.testTag("cancel_booking_${request.id}")
                    )
                } else {
                    Text(
                        text = request.resolvedPriceSummary(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TacticalGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (request.isActive) "Track Live" else "View Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecurityBlueLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SecurityBlueLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
