package com.nevercrynow.ioshub.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 视觉打磨层：按下反馈、骨架屏、入场动画、光晕。
 * 全部用 Compose 自带动画，不引入任何新依赖。
 */

/** 可点击卡片：按下轻微缩小 + 细边框 + 柔和投影，替代默认涟漪。 */
@Composable
fun PressableSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    color: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 3.dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )

    val surfaceColor = color ?: MaterialTheme.colorScheme.surface
    val stroke = borderColor ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.14f))
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, stroke, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        content = content,
    )
}

/** 骨架屏色块：呼吸式明暗，比「正在加载…」文字更有质感。 */
@Composable
fun SkeletonBlock(
    height: Dp = 14.dp,
    corner: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 850), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f + 0.055f * alpha),
                RoundedCornerShape(corner),
            ),
    )
}

/** 首屏入场：轻微上浮 + 淡入，按顺序错开，避免「一下子全砸出来」。 */
@Composable
fun Reveal(index: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, 8) * 45L)
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "reveal",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 14f
        },
    ) {
        content()
    }
}

/** 一层柔光，用来给 Logo / 精选卡背后加「氛围」。 */
@Composable
fun GlowBox(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    alpha: Float = 0.26f,
) {
    Box(
        modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                ),
            ),
    )
}
