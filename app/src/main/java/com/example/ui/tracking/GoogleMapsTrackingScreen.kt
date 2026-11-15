package com.example.ui.tracking

import androidx.compose.runtime.Composable
import com.example.data.model.CommandCenterDeployment
import com.example.data.model.SecurityGuardProfile
import com.example.data.model.SecurityRequest

/**
 * Legacy wrapper preserved for compatibility; delegates directly to [MapScreen].
 */
@Composable
fun GoogleMapsTrackingScreen(
    activeRequest: SecurityRequest? = null,
    activeDeployment: CommandCenterDeployment? = null,
    onNavigateToRadar: (() -> Unit)? = null,
    onDispatchGuardRequested: ((String) -> Unit)? = null,
    onViewGuardProfile: ((SecurityGuardProfile) -> Unit)? = null,
    onSubmitFeedback: ((requestId: String, rating: Int, comment: String?) -> Unit)? = null
) {
    MapScreen(
        nearbyGuards = listOfNotNull(activeDeployment),
        activeRequest = activeRequest,
        activeDeployment = activeDeployment,
        onNavigateToRadar = onNavigateToRadar,
        onDispatchGuardRequested = onDispatchGuardRequested,
        onViewGuardProfile = onViewGuardProfile,
        onSubmitFeedback = onSubmitFeedback
    )
}
