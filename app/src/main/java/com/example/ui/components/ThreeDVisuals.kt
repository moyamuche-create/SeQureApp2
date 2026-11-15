package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleStripeBright
import com.example.ui.theme.PurpleStripeGlow
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.SecurityCardBorder
import com.example.ui.theme.TacticalGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 3D isometric tactile push modifier that creates physical depth,
 * a depressed bevel when pressed, and top specular lighting.
 */
fun Modifier.threeDTactilePush(
    isPressed: Boolean,
    depth: Dp = 4.dp,
    cameraDistance: Float = 14f
): Modifier = this.graphicsLayer {
    val progress = if (isPressed) 1f else 0f
    translationY = (depth * 0.75f * progress).toPx()
    scaleX = 1f - (0.02f * progress)
    scaleY = 1f - (0.02f * progress)
    rotationX = -2f * progress
    this.cameraDistance = cameraDistance
}

/**
 * 3D Animated Icon that floats gently in 3D perspective space with a glossy layered badge.
 */
@Composable
fun ThreeDAnimatedIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    containerSize: Dp = 44.dp,
    tint: Color = Color.White,
    backgroundColors: List<Color> = listOf(SecurityBlueLight, SecurityBluePrimary),
    glowColor: Color = SecurityBlueLight,
    shape: Shape = RoundedCornerShape(12.dp),
    animated: Boolean = true,
    contentDescription: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3d_icon_float")

    val floatY by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3d_icon_float_y"
    )

    val rotY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3d_icon_rot_y"
    )

    val rotX by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3d_icon_rot_x"
    )

    val shineSweep by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "3d_icon_shine_sweep"
    )

    val appliedRotY = if (animated) rotY else 0f
    val appliedRotX = if (animated) rotX else 0f
    val appliedFloatY = if (animated) floatY else 0f

    Box(
        modifier = modifier
            .size(containerSize)
            .graphicsLayer {
                translationY = appliedFloatY
                rotationY = appliedRotY
                rotationX = appliedRotX
                cameraDistance = 14f
            }
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = glowColor.copy(alpha = 0.35f),
                spotColor = glowColor.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(Brush.linearGradient(backgroundColors))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        glowColor.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Specular top highlight line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(4.dp.toPx(), 2.dp.toPx()),
                end = Offset(size.width - 4.dp.toPx(), 2.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Dynamic diagonal light glint sweep
            if (animated) {
                val sweepX = size.width * shineSweep
                val bandW = size.width * 0.4f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - bandW, 0f),
                        end = Offset(sweepX + bandW, size.height)
                    )
                )
            }
        }

        // 3D Shadow layer of the icon itself
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.25f),
            modifier = Modifier
                .size(iconSize)
                .offset(x = 1.dp, y = 1.5.dp)
        )

        // Front 3D icon
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Cutting-edge animated panel featuring:
 * - Glassmorphic high-tech frosted gradient
 * - Animated cyber edge glow border
 * - Interactive 3D depth push/elevation
 * - Optional purple-striped recommended banner
 * - Subtle corner tactical notches
 */
@Composable
fun CuttingEdgePanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = Color.White,
    borderColor: Color = SecurityCardBorder,
    isRecommended: Boolean = false,
    hasAnimatedBorder: Boolean = false,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else elevation,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "panel_elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "panel_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "panel_edge_glow")
    val borderSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "panel_border_sweep"
    )

    val activeBorderBrush = if (isRecommended) {
        val sweepColor1 = PurpleStripeBright
        val sweepColor2 = PurpleStripeGlow
        Brush.sweepGradient(
            colors = listOf(
                sweepColor1,
                sweepColor2.copy(alpha = 0.6f),
                PurplePrimary.copy(alpha = 0.3f),
                sweepColor1
            )
        )
    } else if (hasAnimatedBorder) {
        Brush.sweepGradient(
            colors = listOf(
                SecurityBlueLight,
                PurpleStripeGlow.copy(alpha = 0.5f),
                Color(0xFFE2E8F0),
                SecurityBlueLight
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.9f),
                borderColor
            )
        )
    }

    val finalModifier = modifier
        .scale(scale)
        .shadow(
            elevation = animatedElevation,
            shape = shape,
            ambientColor = if (isRecommended) PurplePrimary.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.05f),
            spotColor = if (isRecommended) PurpleStripeBright.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    containerColor,
                    containerColor.copy(alpha = 0.96f)
                )
            )
        )
        .border(
            width = if (isRecommended) 1.6.dp else 1.dp,
            brush = activeBorderBrush,
            shape = shape
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )

    Column(modifier = finalModifier) {
        if (isRecommended) {
            PurpleStripedBanner(
                height = 4.dp,
                stripeWidthDp = 6f,
                gapWidthDp = 6f,
                animated = true
            )
        }
        content()
    }
}

/**
 * Staggered entrance animation for panels on initial screen load.
 * Delivers smooth cascade motion without stutter.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    delayPerItemMs: Int = 45,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * delayPerItemMs).toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 3D Tactical Button with realistic bottom extrusion layer,
 * physical push-down physics on press, and specular top edge highlight.
 */
@Composable
fun ThreeDTacticalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    baseColor: Color = SecurityBlueLight,
    shadowColor: Color = SecurityBluePrimary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Dp = 50.dp,
    cornerRadius: Dp = 12.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val depth = 4.dp
    val animatedPushY by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) depth * 0.75f else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "3d_button_push_y"
    )

    val currentBevelHeight by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) 1.dp else depth,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "3d_button_bevel"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .heightIn(min = minHeight + depth)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Physical bottom 3D bevel / extrusion base
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
                .offset(y = depth)
                .clip(shape)
                .background(if (enabled) shadowColor else shadowColor.copy(alpha = 0.4f))
        )

        // Top button surface with specular edge and 3D push
        Surface(
            shape = shape,
            color = if (enabled) baseColor else baseColor.copy(alpha = 0.6f),
            shadowElevation = if (isPressed) 0.dp else 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
                .offset(y = animatedPushY)
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationY = if (isPressed) 12f else 0f
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.3.sp
                    )
                )
            }
        }
    }
}

/**
 * 3D Interactive Star Rating with bounce and perspective tilt on tap.
 */
@Composable
fun ThreeDInteractiveStar(
    rating: Int,
    starIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = starIndex <= rating
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "3d_star_scale"
    )

    val rotZ by animateFloatAsState(
        targetValue = if (isSelected) 0f else -12f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "3d_star_rot"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onSelect(starIndex) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Star $starIndex",
            tint = if (isSelected) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
            modifier = Modifier
                .size(34.dp)
                .scale(scale)
                .graphicsLayer {
                    rotationZ = rotZ
                    cameraDistance = 12f
                    rotationX = if (isPressed) 15f else 0f
                }
                .shadow(
                    elevation = if (isSelected) 4.dp else 0.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFFF59E0B).copy(alpha = 0.4f),
                    spotColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
                )
        )
    }
}

/**
 * High-tech Section Header with 3D animated icon badge and cyber status accent.
 */
@Composable
fun CuttingEdgeSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                ThreeDAnimatedIcon(
                    imageVector = icon,
                    containerSize = 36.dp,
                    iconSize = 18.dp,
                    backgroundColors = listOf(PurplePrimary, PurpleStripeBright),
                    glowColor = PurpleStripeGlow
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = NeutralTextPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        if (!badgeText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PurpleSurfaceTint,
                border = BorderStroke(1.dp, PurpleStripeBright.copy(alpha = 0.4f)),
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                    .graphicsLayer { cameraDistance = 10f }
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PurplePrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
