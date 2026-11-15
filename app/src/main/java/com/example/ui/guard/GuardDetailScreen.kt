package com.example.ui.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityGuardProfile
import com.example.ui.components.PurpleStripedBanner
import com.example.ui.components.PurpleStripedRecommendedBadge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NeutralTextMuted
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleBorderTint
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.SecurityNavyDark
import com.example.ui.theme.TacticalGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuardDetailScreen(
    guard: SecurityGuardProfile,
    onBackClicked: () -> Unit,
    onRequestServiceClicked: (SecurityGuardProfile) -> Unit,
    onCallGuardClicked: ((String) -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Officer Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeutralTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier.testTag("guard_detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeutralTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                HorizontalDivider(color = SecurityCardBorder)
            }
        },
        bottomBar = {
            // Sticky Bottom Request Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    HorizontalDivider(color = SecurityCardBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (onCallGuardClicked != null && guard.phone.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onCallGuardClicked(guard.name) },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("guard_call_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SecurityBlueLight),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = SecurityBlueLight
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Guard",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onRequestServiceClicked(guard) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("request_guard_service_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecurityBlueLight,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request ${guard.name.split(" ").lastOrNull() ?: "Officer"}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = SecurityNavyDark
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("guard_profile_header_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        if (guard.isRecommended) 1.5.dp else 1.dp,
                        if (guard.isRecommended) PurpleBorderTint else SecurityCardBorder
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (guard.isRecommended) {
                            PurpleStripedBanner(
                                height = 6.dp,
                                stripeWidthDp = 8f,
                                gapWidthDp = 8f
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile Avatar Stack
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(PurpleSurfaceTint)
                                        .border(2.dp, SecurityBlueLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = guard.avatarInitials,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecurityBlueLight
                                    )
                                }
                                // Verification Badge Icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TacticalGreen)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (guard.isRecommended) {
                                Spacer(modifier = Modifier.height(10.dp))
                                PurpleStripedRecommendedBadge(
                                    text = "RECOMMENDED OFFICER",
                                    modifier = Modifier.testTag("recommended_officer_badge")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = guard.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeutralTextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${guard.serviceType} • Badge ${guard.badgeNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeutralTextSecondary
                            )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TacticalGreen.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TacticalGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (guard.isAvailable) "Active Duty • Ready for Dispatch" else "On Assignment",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TacticalGreen
                                )
                            }
                        }
                    }
                }
            }

                // Metric Highlights Grid (Experience, Rating, Missions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WorkHistory,
                        title = "Experience",
                        value = if (guard.yearsOfExperience > 0) "${guard.yearsOfExperience} Yrs" else "Certified",
                        subtitle = "Field Service",
                        iconColor = SecurityBlueLight
                    )

                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Star,
                        title = "Rating",
                        value = if (guard.rating > 0f) "${guard.rating} ★" else "Verified",
                        subtitle = if (guard.reviewCount > 0) "${guard.reviewCount} Reviews" else "Command Center",
                        iconColor = GoldAccent
                    )

                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Shield,
                        title = "Missions",
                        value = if (guard.completedMissions > 0) "${guard.completedMissions}+" else "Active",
                        subtitle = "Completed",
                        iconColor = TacticalGreen
                    )
                }

                // Biography Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Background & Profile",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SecurityBluePrimary
                        )
                        Text(
                            text = guard.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralTextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Specializations & Skills
                if (guard.specializations.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Specializations & Skills",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SecurityBluePrimary
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                guard.specializations.forEach { spec ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MilitaryTech,
                                                contentDescription = null,
                                                tint = SecurityBlueLight,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = spec,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = NeutralTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Certifications List
                if (guard.certifications.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Verified Certifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SecurityBluePrimary
                            )

                            guard.certifications.forEach { cert ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TacticalGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = cert,
                                        fontSize = 13.sp,
                                        color = NeutralTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Vehicle & Equipment Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurpleSurfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Vehicle",
                                tint = SecurityBlueLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assigned Patrol Vehicle",
                                fontSize = 12.sp,
                                color = NeutralTextSecondary
                            )
                            Text(
                                text = guard.vehicle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeutralTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TacticalGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "GPS Linked",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TacticalGreen
                            )
                        }
                    }
                }

                // Client Reviews Section
                if (guard.reviews.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
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
                                    text = "Client Reviews",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecurityBluePrimary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${guard.rating} / 5.0",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeutralTextPrimary
                                    )
                                }
                            }

                            guard.reviews.forEachIndexed { index, review ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = review.reviewerName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = NeutralTextPrimary
                                        )
                                        Text(
                                            text = review.date,
                                            fontSize = 11.sp,
                                            color = NeutralTextMuted
                                        )
                                    }
                                    Text(
                                        text = review.comment,
                                        fontSize = 13.sp,
                                        color = NeutralTextSecondary
                                    )
                                }

                                if (index < guard.reviews.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = SecurityCardBorder
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SecurityCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = NeutralTextPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeutralTextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = NeutralTextMuted
            )
        }
    }
}
