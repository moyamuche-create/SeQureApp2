package com.example.ui.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityRequest
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen

@Composable
fun PostServiceFeedbackCard(
    request: SecurityRequest,
    onSubmitFeedback: (rating: Int, comment: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var rating by remember(request.id) { mutableIntStateOf(request.rating ?: 5) }
    var comment by remember(request.id) { mutableStateOf(request.feedbackComment ?: "") }
    var isSubmitted by remember(request.id, request.rating) { mutableStateOf(request.rating != null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_service_feedback_card"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (isSubmitted) TacticalGreen.copy(alpha = 0.5f) else SecurityCardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSubmitted) TacticalGreen.copy(alpha = 0.12f)
                                else GoldAccent.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isSubmitted) TacticalGreen.copy(alpha = 0.6f)
                                else GoldAccent.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSubmitted) Icons.Default.CheckCircle else Icons.Default.RateReview,
                            contentDescription = null,
                            tint = if (isSubmitted) TacticalGreen else GoldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isSubmitted) "Feedback Submitted" else "Rate Your Security Service",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NeutralTextPrimary
                        )
                        Text(
                            text = if (isSubmitted) {
                                "Thank you for rating ${request.guardName ?: "your officer"}"
                            } else {
                                "How was ${request.guardName ?: "the officer"}?"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NeutralTextSecondary
                        )
                    }
                }

                if (isSubmitted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TacticalGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, TacticalGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Verified",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TacticalGreen
                        )
                    }
                }
            }

            // Interactive 5-Star Rating Bar (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { starIndex ->
                    val isSelected = starIndex <= rating
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isSubmitted) {
                                rating = starIndex
                            }
                            .testTag("star_rate_$starIndex"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Rate $starIndex stars",
                            tint = if (isSelected) GoldAccent else NeutralBorder,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Feedback rating label
            val ratingLabel = when (rating) {
                5 -> "Outstanding Protection (5.0)"
                4 -> "Very Good Security (4.0)"
                3 -> "Satisfactory Service (3.0)"
                2 -> "Needs Improvement (2.0)"
                else -> "Unsatisfactory (1.0)"
            }

            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = GoldLight,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (!isSubmitted) {
                // Comment text field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = {
                        Text(
                            "Brief comment (e.g. prompt arrival, conduct)",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_comment_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NeutralTextPrimary,
                        unfocusedTextColor = NeutralTextPrimary,
                        focusedLabelColor = SecurityBlueLight,
                        unfocusedLabelColor = NeutralTextSecondary,
                        focusedBorderColor = SecurityBlueLight,
                        unfocusedBorderColor = NeutralBorder,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    maxLines = 3
                )

                // Submit Feedback Button
                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmitFeedback(rating, comment.ifBlank { null })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_feedback_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecurityBlueLight,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Submit Guard Feedback",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            } else if (comment.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, SecurityCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"$comment\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
