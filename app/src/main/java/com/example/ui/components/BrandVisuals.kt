package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PurpleDeep
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleStripeBright
import com.example.ui.theme.PurpleStripeGlow
import com.example.ui.theme.TacticalStripeBlack
import com.example.ui.theme.TacticalStripeWhite

/**
 * SeQure brand emblem built from the 3D shield & padlock logo.
 * Combines purple-striped tactical rim, rotating scanner arcs, and diagonal glint when [animated] is true.
 */
@Composable
fun AnimatedSequreLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    showOrbitalRing: Boolean = true,
    showStripedRim: Boolean = true,
    animated: Boolean = true
) {
    val breatheScale: Float
    val orbitalRotation: Float
    val glintProgress: Float
    val haloGlowAlpha: Float

    if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "sequre_logo_anim")

        val animBreathe by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_breathe_scale"
        )
        val animRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "logo_orbital_rotation"
        )
        val animGlint by infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "logo_glint_sweep"
        )
        val animGlow by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_halo_glow"
        )

        breatheScale = animBreathe
        orbitalRotation = animRotation
        glintProgress = animGlint
        haloGlowAlpha = animGlow
    } else {
        breatheScale = 1.0f
        orbitalRotation = 0f
        glintProgress = -1f
        haloGlowAlpha = 0.35f
    }

    val ringStrokeDp = if (size >= 72.dp) 3.dp else 2.dp
    val innerLogoSize = if (showOrbitalRing) size * 0.84f else size

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showOrbitalRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = ringStrokeDp.toPx()
                val ringRadius = (this.size.minDimension - strokePx) / 2f
                val topLeft = Offset(
                    (this.size.width - ringRadius * 2f) / 2f,
                    (this.size.height - ringRadius * 2f) / 2f
                )
                val arcSize = Size(ringRadius * 2f, ringRadius * 2f)

                drawCircle(
                    color = PurpleStripeBright.copy(alpha = haloGlowAlpha * 0.24f),
                    radius = ringRadius + strokePx * 0.5f
                )

                drawCircle(
                    color = TacticalStripeBlack.copy(alpha = 0.9f),
                    radius = ringRadius,
                    style = Stroke(width = strokePx)
                )

                rotate(degrees = orbitalRotation) {
                    if (showStripedRim) {
                        for (segment in 0 until 8) {
                            drawArc(
                                color = if (segment % 2 == 0) PurpleStripeBright else PurpleStripeGlow,
                                startAngle = segment * 45f,
                                sweepAngle = 16f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx * 0.9f, cap = StrokeCap.Butt)
                            )
                        }
                    }

                    drawArc(
                        color = PurpleStripeGlow.copy(alpha = 0.95f),
                        startAngle = 16f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx * 1.15f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = PurplePrimary,
                        startAngle = 196f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx * 1.15f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(innerLogoSize)
                .scale(breatheScale)
                .clip(CircleShape)
                .background(TacticalStripeBlack)
                .border(1.2.dp, PurpleStripeGlow.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sequre_brand_logo),
                contentDescription = "SeQure Security Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (animated) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = this.size.width
                    val height = this.size.height
                    val sweepCenterX = width * glintProgress
                    val bandWidth = width * 0.35f

                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                TacticalStripeWhite.copy(alpha = 0.2f),
                                PurpleStripeGlow.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            start = Offset(sweepCenterX - bandWidth, 0f),
                            end = Offset(sweepCenterX + bandWidth, height)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Strategic purple-striped tactical security banner.
 * Renders crisp 45-degree diagonal purple stripes over deep obsidian-indigo with optional gentle motion.
 */
@Composable
fun PurpleStripedBanner(
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    stripeWidthDp: Float = 8f,
    gapWidthDp: Float = 8f,
    animated: Boolean = false,
    baseColor: Color = PurpleDeep,
    stripeColor: Color = PurpleStripeBright
) {
    val phaseShift = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "purple_striped_banner_anim")
        val shift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "purple_stripe_phase"
        )
        shift
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        drawRect(color = baseColor)

        val stripeWidthPx = stripeWidthDp.dp.toPx()
        val gapWidthPx = gapWidthDp.dp.toPx()
        val pitch = stripeWidthPx + gapWidthPx
        val slantOffset = size.height
        val animatedOffsetPx = phaseShift * pitch

        var startX = -slantOffset - pitch + animatedOffsetPx
        while (startX < size.width + pitch) {
            val stripePath = Path().apply {
                moveTo(startX, size.height)
                lineTo(startX + stripeWidthPx, size.height)
                lineTo(startX + stripeWidthPx + slantOffset, 0f)
                lineTo(startX + slantOffset, 0f)
                close()
            }
            drawPath(path = stripePath, color = stripeColor)
            startX += pitch
        }
    }
}

/**
 * Compact tactical badge pairing deep purple label surface with crisp purple-striped edge accents.
 */
@Composable
fun PurpleStripedHeaderBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = PurpleDeep
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            PurpleStripedEdgeBlock(
                width = 18.dp,
                height = 22.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = TacticalStripeWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PurpleStripedEdgeBlock(
                width = 18.dp,
                height = 22.dp
            )
        }
    }
}

/**
 * High-visibility "RECOMMENDED" badge featuring diagonal purple stripes for preferred tiers,
 * service packages, and top-rated officers.
 */
@Composable
fun PurpleStripedRecommendedBadge(
    text: String = "RECOMMENDED",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = PurpleDeep
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(6.dp))
        ) {
            PurpleStripedEdgeBlock(
                width = 10.dp,
                height = 20.dp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PurpleStripeGlow,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    color = TacticalStripeWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp
                )
            }
            PurpleStripedEdgeBlock(
                width = 10.dp,
                height = 20.dp
            )
        }
    }
}

@Composable
private fun PurpleStripedEdgeBlock(
    width: Dp,
    height: Dp
) {
    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
            .clipToBounds()
    ) {
        drawRect(color = PurpleDeep)
        val stripeWidthPx = 4.dp.toPx()
        val pitch = 8.dp.toPx()
        val slantOffset = size.height * 0.65f
        var x = -slantOffset
        while (x < size.width + pitch) {
            val path = Path().apply {
                moveTo(x, size.height)
                lineTo(x + stripeWidthPx, size.height)
                lineTo(x + stripeWidthPx + slantOffset, 0f)
                lineTo(x + slantOffset, 0f)
                close()
            }
            drawPath(path = path, color = PurpleStripeBright)
            x += pitch
        }
    }
}
