package com.nevercrynow.ioshub.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 打开应用时的品牌动画（约 1.5 秒）。
 *
 * 编排顺序：底光晕先亮 → 图标从 0.72 弹簧放大 → 三条叠片依次"落笔"绘出
 * → "iOS HUB" 字样上浮淡入 → "by nevercrynow" → 副标题 → 底部进度条走完
 * → 整层放大淡出，露出首页。
 *
 * 所有动画都用 [Animatable] 手动编排，而不是一条无限循环 —— 这样每一段
 * 的相对时序可控，且播完自动卸载，不会在后台白耗帧。
 */
@Composable
fun LaunchOverlay(onFinished: () -> Unit) {
    val markAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.72f) }
    val draw = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordShift = remember { Animatable(14f) }
    val byAlpha = remember { Animatable(0f) }
    val tagAlpha = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    // 献给赵菁然：最后出现，停留一会儿再一起淡出
    val dedicAlpha = remember { Animatable(0f) }
    val dedicShift = remember { Animatable(10f) }
    val heartScale = remember { Animatable(0.4f) }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { markAlpha.animateTo(1f, tween(260, easing = LinearEasing)) }
            launch { markScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
            launch { glow.animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }
            launch { bar.animateTo(1f, tween(1320, easing = LinearEasing)) }
            launch {
                delay(170)
                draw.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                delay(390)
                launch { wordAlpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }
                wordShift.animateTo(0f, tween(440, easing = FastOutSlowInEasing))
            }
            launch {
                delay(540)
                byAlpha.animateTo(1f, tween(300))
            }
            launch {
                delay(670)
                tagAlpha.animateTo(1f, tween(320))
            }
            launch {
                delay(880)
                launch { dedicAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
                launch { heartScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                dedicShift.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
            }
            delay(1560)
            exit.animateTo(0f, tween(360, easing = FastOutLinearInEasing))
        }
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .graphicsLayer {
                alpha = exit.value
                val grow = 1f + 0.06f * (1f - exit.value)
                scaleX = grow
                scaleY = grow
            }
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 背景光晕：从很小的一团慢慢撑开，给图标一个"浮起来"的底座
        Box(
            Modifier
                .align(Alignment.Center)
                .size(340.dp)
                .graphicsLayer {
                    alpha = 0.32f * glow.value
                    val s = 0.45f + 0.65f * glow.value
                    scaleX = s
                    scaleY = s
                }
                .background(
                    Brush.radialGradient(
                        listOf(Brand, Violet.copy(alpha = 0.55f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            HubMark(
                size = 96.dp,
                progress = draw.value,
                modifier = Modifier.graphicsLayer {
                    alpha = markAlpha.value
                    scaleX = markScale.value
                    scaleY = markScale.value
                },
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                    translationY = wordShift.value
                },
            ) {
                Text(
                    "iOS HUB",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.graphicsLayer { alpha = byAlpha.value }) {
                Text(
                    "by nevercrynow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand,
                    letterSpacing = 0.4.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.graphicsLayer { alpha = tagAlpha.value }) {
                Text(
                    "全生态 iOS 资源中心",
                    fontSize = 11.5f.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.graphicsLayer {
                    alpha = dedicAlpha.value
                    translationY = dedicShift.value
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "献给",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "赵菁然",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Rose,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.width(6.dp))
                GlyphIcon(
                    glyph = Glyph.Heart,
                    size = 11.dp,
                    tint = Rose.copy(alpha = 0.85f),
                    filled = true,
                    modifier = Modifier.graphicsLayer {
                        scaleX = heartScale.value
                        scaleY = heartScale.value
                    },
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(width = 132.dp, height = 3.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f), CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(bar.value.coerceIn(0f, 1f))
                        .background(BrandBrush, CircleShape),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "NEVERCRYNOW",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                letterSpacing = 3.2.sp,
            )
        }
    }
}
