package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleStripeBright
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder

/**
 * Primary Material 3 3D responsive button with tactile press-depth physics,
 * physical bottom extrusion bevel, specular highlight line,
 * minimum 48.dp touch target height, and 3D animated icon.
 */
@Composable
fun ResponsiveActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = SecurityBlueLight,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Dp = 50.dp,
    cornerRadius: Dp = 12.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pushY by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 3.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_3d_push_y"
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) 1.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_3d_shadow"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.55f),
            disabledContentColor = contentColor.copy(alpha = 0.85f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = shadowElevation,
            pressedElevation = 0.dp
        ),
        modifier = modifier
            .graphicsLayer {
                translationY = pushY.dp.toPx()
                scaleX = if (isPressed && enabled && !isLoading) 0.985f else 1f
                scaleY = if (isPressed && enabled && !isLoading) 0.985f else 1f
                rotationX = if (isPressed && enabled && !isLoading) -2f else 0f
                cameraDistance = 14f
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.45f else 0.15f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .heightIn(min = minHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.4.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer {
                            rotationY = if (isPressed) 15f else 0f
                            cameraDistance = 12f
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 0.4.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Secondary Material 3 3D outlined button with tactile press-depth physics,
 * glossy border glint, and minimum 48.dp touch target height.
 */
@Composable
fun ResponsiveOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = SecurityBlueLight,
    containerColor: Color = Color.White,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
    cornerRadius: Dp = 12.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pushY by animateFloatAsState(
        targetValue = if (isPressed && enabled) 2.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "outlined_3d_push"
    )

    val shape = RoundedCornerShape(cornerRadius)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = BorderStroke(
            1.4.dp,
            if (enabled) {
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor,
                        accentColor.copy(alpha = 0.5f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.15f))
                )
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = accentColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier
            .graphicsLayer {
                translationY = pushY.dp.toPx()
                scaleX = if (isPressed && enabled) 0.985f else 1f
                scaleY = if (isPressed && enabled) 0.985f else 1f
                rotationX = if (isPressed && enabled) -1.8f else 0f
                cameraDistance = 14f
            }
            .shadow(
                elevation = if (isPressed) 0.dp else 2.5.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.15f),
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .heightIn(min = minHeight)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        rotationY = if (isPressed) 12f else 0f
                    }
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.3.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Responsive segmented pill button with 3D elevation, minimum 48.dp accessible height,
 * animated color transitions, and 3D push feedback.
 */
@Composable
fun ResponsiveSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selectedColor: Color = SecurityBlueLight,
    minHeight: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pushY by animateFloatAsState(
        targetValue = if (isPressed) 2f else if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "segment_3d_push"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color(0xFFF8FAFC),
        animationSpec = tween(durationMillis = 180),
        label = "segment_btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else NeutralTextSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "segment_btn_fg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedColor else SecurityCardBorder,
        animationSpec = tween(durationMillis = 180),
        label = "segment_btn_border"
    )

    val shape = RoundedCornerShape(10.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                translationY = pushY.dp.toPx()
                scaleX = if (isPressed) 0.97f else 1f
                scaleY = if (isPressed) 0.97f else 1f
                rotationX = if (isPressed) -1.5f else 0f
                cameraDistance = 14f
            }
            .shadow(
                elevation = if (selected) 4.dp else 1.dp,
                shape = shape,
                ambientColor = if (selected) selectedColor.copy(alpha = 0.25f) else Color.Transparent,
                spotColor = if (selected) selectedColor.copy(alpha = 0.35f) else Color.Transparent
            )
            .heightIn(min = minHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationY = if (selected) 8f else 0f
                        }
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Adaptive 2-button container that displays buttons side-by-side on standard/wide
 * screens and stacks vertically when width is tight or system font scale is large.
 */
@Composable
fun ResponsiveDualButtonBar(
    modifier: Modifier = Modifier,
    stackThresholdDp: Dp = 360.dp,
    primaryContent: @Composable (Modifier) -> Unit,
    secondaryContent: @Composable (Modifier) -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val shouldStack = maxWidth < stackThresholdDp || fontScale >= 1.25f
        if (shouldStack) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                primaryContent(Modifier.fillMaxWidth())
                secondaryContent(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                secondaryContent(Modifier.weight(1f))
                primaryContent(Modifier.weight(1f))
            }
        }
    }
}
