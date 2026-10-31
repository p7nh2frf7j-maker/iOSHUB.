package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.Category

/**
 * 界面里的小图标，全部用 Canvas 手绘。
 *
 * 为什么不用 emoji：emoji 在不同厂商 ROM 上是不同字体（OEM 还会替换），
 * 同一份设计在小米、三星、Pixel 上会跳到完全不同的形状和颜色，没法控制
 * 视觉；也不受深浅色主题影响。这里统一在 24×24 的坐标格里用描边路径绘制，
 * 只吃 tint 颜色，任何密度下都清晰。
 */
enum class Glyph {
    Search,
    Bell,
    Dice,
    News,
    Shield,
    Warning,
    Spark,
    Sliders,
    Refresh,
    Globe,
    Dot,
    Heart,
    Check,
}

@Composable
fun GlyphIcon(
    glyph: Glyph,
    size: Dp = 16.dp,
    tint: Color = LocalContentColor.current,
    strokeWidth: Dp = 1.7.dp,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        drawGlyph(glyph, tint, strokeWidth.toPx(), filled)
    }
}

/** 在 [DrawScope] 里画一个图标；坐标按 24×24 设计格等比缩放到实际尺寸。 */
fun DrawScope.drawGlyph(
    glyph: Glyph,
    tint: Color,
    strokeWidth: Float,
    filled: Boolean = false,
) {
    val k = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (glyph) {
        Glyph.Search -> {
            drawCircle(tint, radius = 6.1f * k, center = p(10.6f, 10.6f), style = stroke)
            drawLine(tint, p(15.2f, 15.2f), p(19.8f, 19.8f), stroke.width, StrokeCap.Round)
        }

        Glyph.Bell -> {
            val body = Path().apply {
                moveTo(7.2f * k, 16.4f * k)
                lineTo(7.2f * k, 11.5f * k)
                cubicTo(7.2f * k, 8.6f * k, 9.3f * k, 6.6f * k, 12f * k, 6.6f * k)
                cubicTo(14.7f * k, 6.6f * k, 16.8f * k, 8.6f * k, 16.8f * k, 11.5f * k)
                lineTo(16.8f * k, 16.4f * k)
            }
            drawPath(body, tint, style = stroke)
            drawLine(tint, p(5.9f, 16.5f), p(18.1f, 16.5f), stroke.width, StrokeCap.Round)
            drawArc(
                color = tint,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = p(9.7f, 17.5f),
                size = Size(4.6f * k, 4.6f * k),
                style = stroke,
            )
            drawCircle(tint, radius = 1.25f * k, center = p(12f, 5.3f))
        }

        Glyph.Dice -> {
            drawRoundRect(
                color = tint,
                topLeft = p(4.3f, 4.3f),
                size = Size(15.4f * k, 15.4f * k),
                cornerRadius = CornerRadius(4.2f * k),
                style = stroke,
            )
            drawCircle(tint, radius = 1.45f * k, center = p(9f, 9f))
            drawCircle(tint, radius = 1.45f * k, center = p(15f, 15f))
            drawCircle(tint, radius = 1.45f * k, center = p(15f, 9f))
            drawCircle(tint, radius = 1.45f * k, center = p(9f, 15f))
        }

        Glyph.News -> {
            drawRoundRect(
                color = tint,
                topLeft = p(3.8f, 5.6f),
                size = Size(16.4f * k, 13.2f * k),
                cornerRadius = CornerRadius(2.6f * k),
                style = stroke,
            )
            drawLine(tint, p(7.4f, 9.2f), p(7.4f, 15.2f), stroke.width, StrokeCap.Round)
            drawLine(tint, p(10.2f, 9.6f), p(16.8f, 9.6f), stroke.width, StrokeCap.Round)
            drawLine(tint, p(10.2f, 12.4f), p(16.8f, 12.4f), stroke.width, StrokeCap.Round)
            drawLine(tint, p(10.2f, 15.2f), p(13.8f, 15.2f), stroke.width, StrokeCap.Round)
        }

        Glyph.Shield -> {
            val shield = Path().apply {
                moveTo(12f * k, 3.1f * k)
                lineTo(19.6f * k, 6.2f * k)
                lineTo(19.6f * k, 12f * k)
                cubicTo(19.6f * k, 16.2f * k, 16.3f * k, 19.4f * k, 12f * k, 20.9f * k)
                cubicTo(7.7f * k, 19.4f * k, 4.4f * k, 16.2f * k, 4.4f * k, 12f * k)
                lineTo(4.4f * k, 6.2f * k)
                close()
            }
            drawPath(shield, tint, style = stroke)
            if (filled) {
                drawPath(shield, tint.copy(alpha = 0.16f))
            }
            val check = Path().apply {
                moveTo(8.4f * k, 11.9f * k)
                lineTo(11f * k, 14.5f * k)
                lineTo(15.7f * k, 9.6f * k)
            }
            drawPath(check, tint, style = stroke)
        }

        Glyph.Warning -> {
            val tri = Path().apply {
                moveTo(12f * k, 3.4f * k)
                lineTo(21.2f * k, 19.6f * k)
                lineTo(2.8f * k, 19.6f * k)
                close()
            }
            drawPath(tri, tint, style = stroke)
            drawLine(tint, p(12f, 9.2f), p(12f, 14.2f), stroke.width, StrokeCap.Round)
            drawCircle(tint, radius = 1.15f * k, center = p(12f, 16.9f))
        }

        Glyph.Spark -> {
            val star = Path().apply {
                moveTo(12f * k, 2.8f * k)
                lineTo(14.3f * k, 9.7f * k)
                lineTo(21.2f * k, 12f * k)
                lineTo(14.3f * k, 14.3f * k)
                lineTo(12f * k, 21.2f * k)
                lineTo(9.7f * k, 14.3f * k)
                lineTo(2.8f * k, 12f * k)
                lineTo(9.7f * k, 9.7f * k)
                close()
            }
            drawPath(star, tint, style = stroke)
        }

        Glyph.Sliders -> {
            drawLine(tint, p(3.8f, 8.4f), p(20.2f, 8.4f), stroke.width, StrokeCap.Round)
            drawCircle(tint, radius = 2.3f * k, center = p(9.2f, 8.4f), style = stroke)
            drawLine(tint, p(3.8f, 15.8f), p(20.2f, 15.8f), stroke.width, StrokeCap.Round)
            drawCircle(tint, radius = 2.3f * k, center = p(15f, 15.8f), style = stroke)
        }

        Glyph.Refresh -> {
            drawArc(
                color = tint,
                startAngle = -55f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = p(4.2f, 4.2f),
                size = Size(15.6f * k, 15.6f * k),
                style = stroke,
            )
            val head = Path().apply {
                moveTo(17.3f * k, 2.9f * k)
                lineTo(20.6f * k, 6.6f * k)
                lineTo(16.1f * k, 7.4f * k)
                close()
            }
            drawPath(head, tint)
        }

        Glyph.Globe -> {
            drawCircle(tint, radius = 8.2f * k, center = p(12f, 12f), style = stroke)
            drawOval(
                color = tint,
                topLeft = p(8.5f, 3.8f),
                size = Size(7f * k, 16.4f * k),
                style = stroke,
            )
            drawLine(tint, p(3.8f, 12f), p(20.2f, 12f), stroke.width, StrokeCap.Round)
        }

        Glyph.Dot -> drawCircle(tint, radius = 3.2f * k, center = p(12f, 12f))

        Glyph.Heart -> {
            val heart = Path().apply {
                moveTo(12f * k, 20.4f * k)
                cubicTo(12f * k, 20.4f * k, 3.6f * k, 15.2f * k, 3.6f * k, 9.8f * k)
                cubicTo(3.6f * k, 7.1f * k, 5.7f * k, 5.2f * k, 8.2f * k, 5.2f * k)
                cubicTo(9.9f * k, 5.2f * k, 11.3f * k, 6.1f * k, 12f * k, 7.4f * k)
                cubicTo(12.7f * k, 6.1f * k, 14.1f * k, 5.2f * k, 15.8f * k, 5.2f * k)
                cubicTo(18.3f * k, 5.2f * k, 20.4f * k, 7.1f * k, 20.4f * k, 9.8f * k)
                cubicTo(20.4f * k, 15.2f * k, 12f * k, 20.4f * k, 12f * k, 20.4f * k)
                close()
            }
            if (filled) {
                drawPath(heart, tint)
            } else {
                drawPath(heart, tint, style = stroke)
            }
        }

        Glyph.Check -> {
            val check = Path().apply {
                moveTo(5f * k, 12.6f * k)
                lineTo(9.8f * k, 17.4f * k)
                lineTo(19f * k, 6.8f * k)
            }
            drawPath(check, tint, style = stroke)
        }
    }
}

// MARK: - 分类字标

/**
 * 分类的两位字标。用拉丁字母而不是中文首字，因为「开源」和「开发」
 * 首字都是「开」，会撞车；两位缩写同时也更像 App 图标。
 */
fun Category.mark(): String = when (this) {
    Category.JAILBREAK -> "JB"
    Category.TROLLSTORE -> "TS"
    Category.IPA -> "IP"
    Category.SIDELOAD -> "SD"
    Category.TWEAK -> "TW"
    Category.REPO -> "RP"
    Category.OPENSOURCE -> "OS"
    Category.SECURITY -> "SC"
    Category.DEVTOOLS -> "DX"
    Category.THEMING -> "TH"
    Category.UTILITIES -> "UT"
    Category.PORT -> "PT"
    Category.NEWS -> "NW"
}

/**
 * 分类图标：单色 tint 的渐变小方块 + 白色字标。
 * 比 emoji 稳，也比 13 个手绘图标的实现成本低。
 */
@Composable
fun CategoryMark(category: Category, size: Dp, radius: Dp? = null) {
    val tint = category.tintColor()
    MonogramMark(
        text = category.mark(),
        tint = tint,
        size = size,
        radius = radius ?: (size * 0.30f),
    )
}

/**
 * 通用字标方块：渐变底 + 白字。用于分类、资讯源、风险等级等一切
 * 以前用 emoji 当图标的地方。
 */
@Composable
fun MonogramMark(
    text: String,
    tint: Color,
    size: Dp,
    radius: Dp? = null,
    alpha: Float = 1f,
) {
    val shape = RoundedCornerShape(radius ?: (size * 0.30f))
    Box(
        modifier = Modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(
                    listOf(tint.lighten(0.10f), tint, tint.darken(0.28f)),
                ),
                shape = shape,
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = alpha),
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
