package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃（Liquid Glass）。
 *
 * Android 上真正的模糊代价很高（需要 API 31 的 RenderEffect，且离屏合成会掉帧），
 * 所以这里用「半透明分层 + 高光边框 + 柔和投影 + 底层光斑」来模拟玻璃的观感：
 *
 * 1. 页面底层画 4 团大半径径向渐变光斑（[Modifier.glassPage]），颜色是品牌色系；
 * 2. 卡片是白色的低透明度垂直渐变，叠在光斑上，于是每张卡片都会「折射」到不同的底色；
 * 3. 边框用「左上白 → 中间品牌色 → 右下几乎透明」的线性渐变，模拟玻璃边缘的反光；
 * 4. 投影用品牌色而非纯黑，避免灰扑扑的脏感。
 *
 * 这样在深色模式下也不会糊成一片：深色用白色 6%–16% 的透明度叠在黑底上。
 */
object Glass {
    val radius: Dp = 22.dp

    /** 用于判断当前是深色还是浅色，避免每个调用点都传 Context。 */
    @Composable
    internal fun isDark(): Boolean =
        MaterialTheme.colorScheme.background.luminance() < 0.5f
}

/** 页面底色的光斑：四团径向渐变，模拟被玻璃折射的彩色环境光。 */
fun DrawScope.drawAmbientBlobs(dark: Boolean) {
    val weight = if (dark) 0.85f else 1f
    val min = size.minDimension
    glassBlob(Brand.copy(alpha = 0.30f * weight), Offset(size.width * 0.10f, size.height * 0.04f), min * 0.66f)
    glassBlob(Violet.copy(alpha = 0.24f * weight), Offset(size.width * 1.00f, size.height * 0.20f), min * 0.58f)
    glassBlob(Mint.copy(alpha = 0.16f * weight), Offset(size.width * 0.02f, size.height * 0.74f), min * 0.52f)
    glassBlob(Rose.copy(alpha = 0.13f * weight), Offset(size.width * 0.88f, size.height * 0.98f), min * 0.46f)
}

private fun DrawScope.glassBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color,
            0.55f to color.copy(alpha = color.alpha * 0.45f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * 给整页套上「液态玻璃」底色。用法和原来的 `.background(background)` 一样，只是要多铺一层光斑。
 */
@Composable
fun Modifier.glassPage(): Modifier {
    val dark = Glass.isDark()
    val base = MaterialTheme.colorScheme.background
    return this
        .background(base)
        .drawBehind { drawAmbientBlobs(dark) }
}

/**
 * 一块玻璃面板。默认圆角 22dp、白 46%–82% 的垂直渐变、品牌色投影。
 *
 * @param tint 决定投影颜色与边框色调，一般传资源分类色或 [Brand]。
 * @param glowing 是否带品牌色外发光（浮在内容之上的导航栏、底部主按钮用）。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = Glass.radius,
    tint: Color = Brand,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = Glass.isDark()
    val shape: Shape = RoundedCornerShape(corner)
    val fill = if (dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.05f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.86f), Color.White.copy(alpha = 0.52f)))
    }
    val edge = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) 0.30f else 0.95f),
        0.45f to tint.copy(alpha = if (dark) 0.22f else 0.16f),
        1f to Color.White.copy(alpha = 0.04f),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = tint.copy(alpha = if (dark) 0.22f else 0.30f),
                spotColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.14f),
            )
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, edge), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * 玻璃质感的顶栏：内容滚动到它下面时会被「磨砂条」压住，视觉上像悬浮的胶囊。
 */
@Composable
fun GlassTopBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = Glass.isDark()
    val tint = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        tint.copy(alpha = if (dark) 0.86f else 0.92f),
                        tint.copy(alpha = if (dark) 0.62f else 0.72f),
                    ),
                ),
            ),
    ) {
        content()
        // 底部的 1 像素高光，玻璃片下缘的反射
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (dark) 0.18f else 0.70f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/**
 * 玻璃胶囊（导航栏、筛选条、快捷按钮）。
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    tint: Color = Brand,
    content: @Composable () -> Unit,
) {
    val dark = Glass.isDark()
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = tint.copy(alpha = if (dark) 0.26f else 0.34f),
                spotColor = Color.Black.copy(alpha = if (dark) 0.50f else 0.16f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.18f else 0.92f),
                        Color.White.copy(alpha = if (dark) 0.07f else 0.66f),
                    ),
                ),
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.32f else 1f),
                        0.5f to tint.copy(alpha = 0.14f),
                        1f to Color.White.copy(alpha = 0.05f),
                    ),
                ),
                shape,
            ),
    ) {
        content()
    }
}

/**
 * 只给「一个已有组件」套玻璃底（搜索框、筛选 chip、小按钮）。
 * 和 [GlassSurface] 的区别：不加 Column，不改布局，纯粹画背景 + 描边 + 投影。
 */
@Composable
fun Modifier.glassCapsule(corner: Dp = 26.dp, tint: Color = Brand): Modifier {
    val dark = Glass.isDark()
    val shape = RoundedCornerShape(corner)
    val fill = if (dark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.90f), Color.White.copy(alpha = 0.66f)))
    }
    val edge = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) 0.28f else 0.95f),
        0.5f to tint.copy(alpha = 0.14f),
        1f to Color.White.copy(alpha = 0.04f),
    )
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            clip = false,
            ambientColor = tint.copy(alpha = if (dark) 0.16f else 0.20f),
            spotColor = Color.Black.copy(alpha = if (dark) 0.35f else 0.08f),
        )
        .clip(shape)
        .background(fill)
        .border(BorderStroke(1.dp, edge), shape)
}

/**
 * 全屏玻璃背景（启动页、空态页用）。比 [Modifier.glassPage] 多一层中心光晕。
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = Glass.isDark()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().drawBehind { drawAmbientBlobs(dark) })
        content()
    }
}
