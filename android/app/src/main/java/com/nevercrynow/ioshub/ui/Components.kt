package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.Category
import com.nevercrynow.ioshub.data.GitHubApi
import com.nevercrynow.ioshub.data.ResourceItem

// MARK: - 卡片

@Composable
fun HubCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        corner = 20.dp,
        contentPadding = PaddingValues(padding),
        content = content,
    )
}

// MARK: - 品牌

/** 品牌图标：委托 [HubMark]，形状定义在 BrandMark.kt 里，iOS 版用同一套几何参数。 */
@Composable
fun BrandLogo(size: Dp = 42.dp, showsShadow: Boolean = true) {
    HubMark(size = size, showsShadow = showsShadow)
}

@Composable
fun BrandSignature(titleSize: Int = 24) {
    Row {
        Text(
            text = "iOS HUB",
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "by nevercrynow",
            fontSize = (titleSize * 0.4f).sp,
            fontWeight = FontWeight.SemiBold,
            color = Brand.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.Bottom).padding(bottom = 3.dp),
        )
    }
}

@Composable
fun BrandFooter(note: String = "用 Kotlin + Compose 写的 iOS 生态资源浏览器 · 不托管安装包") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(size = 22.dp, showsShadow = false)
            Spacer(Modifier.width(8.dp))
            Text("iOS HUB", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(
                "by nevercrynow",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand.copy(alpha = 0.85f),
            )
        }
        Text(
            note,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - 标签

@Composable
fun Pill(text: String, tint: Color = Brand, filled: Boolean = false) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (filled) Color.White else tint,
        modifier = Modifier
            .background(if (filled) tint else tint.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

// MARK: - 区块标题

@Composable
fun SectionHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 3.dp, height = 13.dp)
                        .background(Brand, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(7.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp, top = 1.dp),
                )
            }
        }
        if (trailing != null) trailing()
    }
}

// MARK: - 搜索入口

@Composable
fun SearchEntry(placeholder: String = "搜索工具、插件、IPA、开发者或 iOS 版本", onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(Glyph.Search, size = 15.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 1.6.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                placeholder,
                fontSize = 12.5f.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            GlyphIcon(Glyph.Sliders, size = 15.dp, tint = Brand)
        }
    }
}

// MARK: - 提示条

@Composable
fun NoticeBanner(message: String, tint: Color = Amber, onRetry: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.Warning, size = 14.dp, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Spacer(Modifier.width(8.dp))
            Text("重试", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint, modifier = Modifier.clickable(onClick = onRetry))
        }
    }
}

// MARK: - 空态

@Composable
fun EmptyState(glyph: Glyph, title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Brand.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, size = 20.dp, tint = Brand, strokeWidth = 1.8.dp)
        }
        Spacer(Modifier.height(2.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            message,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// MARK: - 分类

@Composable
fun CategoryTile(category: Category, onClick: () -> Unit) {
    val tint = category.tintColor()
    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryMark(category = category, size = 46.dp, radius = 14.dp)
        Text(category.short, fontSize = 10.sp, maxLines = 1)
    }
}

// MARK: - 资源行

@Composable
fun ResourceRow(item: ResourceItem, onClick: () -> Unit) {
    val tint = item.category.tintColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryMark(category = item.category, size = 44.dp, radius = 13.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.summary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.title, fontSize = 9.5f.sp, color = tint)
                Text(" · ", fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    item.stars?.let { "★ ${compact(it)}" } ?: item.compatibility.label,
                    fontSize = 9.5f.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// MARK: - 横向卡片

@Composable
fun ResourceCard(item: ResourceItem, onClick: () -> Unit) {
    val tint = item.category.tintColor()
    PressableSurface(
        modifier = Modifier
            .width(232.dp)
            .height(168.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryMark(category = item.category, size = 36.dp, radius = 11.dp)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        item.developer,
                        fontSize = 9.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.summary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(item.category.short, tint)
                Spacer(Modifier.width(5.dp))
                if (item.stars != null) {
                    Pill("★ ${compact(item.stars)}", Amber)
                } else {
                    Pill(item.compatibility.label, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// MARK: - GitHub 仓库行

@Composable
fun GitHubRepoRow(repo: GitHubApi.Repo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonogramMark(text = "GH", tint = Slate, size = 36.dp, radius = 11.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(repo.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!repo.description.isNullOrBlank()) {
                Text(
                    repo.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row {
                Text("★ ${compact(repo.stars)}", fontSize = 9.5f.sp, color = Amber)
                if (repo.language != null) {
                    Text(" · ${repo.language}", fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 12_345 -> "1.2万" */
fun compact(value: Int): String {
    if (value < 10_000) return value.toString()
    val wan = value / 10_000.0
    return String.format("%.1f万", wan)
}
