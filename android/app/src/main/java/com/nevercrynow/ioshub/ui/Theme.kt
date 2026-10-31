package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.Category

// 品牌色（与 iOS 版一一对应）
val Brand = Color(0xFF5466FA)
val BrandDeep = Color(0xFF783FF0)
val Violet = Color(0xFF9E6BFA)
val Mint = Color(0xFF29C299)
val Amber = Color(0xFFF7AD38)
val Rose = Color(0xFFF25C75)
val Slate = Color(0xFF6B7894)

val BrandBrush = Brush.linearGradient(listOf(Brand, BrandDeep, Violet))

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E9FF),
    onPrimaryContainer = BrandDeep,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF78787F),
    outline = Color(0xFFE7E7EC),
    error = Rose,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E99FF),
    onPrimary = Color(0xFF0B0B10),
    primaryContainer = Color(0xFF2A2A3A),
    onPrimaryContainer = Color(0xFFDDE0FF),
    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFF3F3F7),
    surface = Color(0xFF16161C),
    onSurface = Color(0xFFF3F3F7),
    surfaceVariant = Color(0xFF202029),
    onSurfaceVariant = Color(0xFFA3A3AE),
    outline = Color(0xFF2C2C36),
    error = Color(0xFFFF8A9B),
)

val HubTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
    ),
)

@Composable
fun IosHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = HubTypography,
        content = content,
    )
}

/** 分类专属色。 */
fun Category.tintColor(): Color = Color(this.tint)
