package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * iOS HUB 的品牌标记：渐变圆角方块 + 三级阶梯叠片。
 *
 * 全部用 Canvas 画，不依赖图片资源，也不依赖 emoji 字体 —— 这样在
 * 任何设备、任何密度下都是同一个形状。叠片从左向右逐级下沉并向右
 * 伸出，读起来像「一层层堆起来的资源」，也就是 Hub 的意思。
 *
 * 坐标统一在 0..1 的单位空间里，画之前乘实际边长，保证任意尺寸都不失真。
 */
private val Slabs = listOf(
    // 起点 x, 终点 x, y, 透明度
    Slab(0.235f, 0.585f, 0.265f, 1.00f),
    Slab(0.330f, 0.720f, 0.437f, 1.00f),
    Slab(0.425f, 0.765f, 0.609f, 0.94f),
)

private data class Slab(val from: Float, val to: Float, val y: Float, val alpha: Float)

private const val SLAB_HEIGHT = 0.112f
private const val SLAB_CORNER = 0.056f
private const val TILE_CORNER = 0.300f

/**
 * 在 [DrawScope] 里画品牌标记。
 * @param progress 0..1，控制三条叠片依次「长出来」的进度（用于启动动画）。
 */
fun DrawScope.drawHubMark(progress: Float = 1f, showsGloss: Boolean = true) {
    val s = size.minDimension
    if (s <= 0f) return

    // 1. 底：品牌渐变方块
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Brand, BrandDeep, Violet),
            start = Offset(0f, 0f),
            end = Offset(s, s),
        ),
        topLeft = Offset.Zero,
        size = Size(s, s),
        cornerRadius = CornerRadius(s * TILE_CORNER),
    )

    // 2. 左上角柔光，让纯渐变有一点立体感
    if (showsGloss) {
        val c = Offset(s * 0.26f, s * 0.18f)
        val r = s * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
    }

    // 3. 三级叠片，逐条绘出
    Slabs.forEachIndexed { index, slab ->
        val window = 0.62f
        val local = ((progress - index * 0.18f) / window).coerceIn(0f, 1f)
        if (local <= 0f) return@forEachIndexed
        val eased = 1f - (1f - local) * (1f - local) // easeOutQuad
        val width = (slab.to - slab.from) * s * eased
        drawRoundRect(
            color = Color.White.copy(alpha = slab.alpha),
            topLeft = Offset(slab.from * s, slab.y * s),
            size = Size(width, s * SLAB_HEIGHT),
            cornerRadius = CornerRadius(s * SLAB_CORNER),
        )
    }
}

/**
 * 品牌标记组件。[progress] 小于 1 时会呈现「正在绘制」的中间态，
 * 启动动画用它做逐条落笔的效果。
 */
@Composable
fun HubMark(
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    showsShadow: Boolean = true,
) {
    val shape = RoundedCornerShape(size * TILE_CORNER)
    val elevation = if (showsShadow) size * 0.20f else 0.dp
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Brand.copy(alpha = 0.35f),
                spotColor = BrandDeep.copy(alpha = 0.40f),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawHubMark(progress = progress)
        }
    }
}

/** 把颜色按比例压暗，用于给单色 tint 造一个渐变。 */
internal fun Color.darken(fraction: Float = 0.22f): Color = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha,
)

internal fun Color.lighten(fraction: Float = 0.18f): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha,
)
