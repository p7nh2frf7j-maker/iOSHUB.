package com.nevercrynow.ioshub.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.Category
import com.nevercrynow.ioshub.data.ResourceItem
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    state: AppState,
    onOpenDetail: (ResourceItem) -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNews: () -> Unit,
) {
    // 数据还没读出来时只显示骨架，避免「空页面一闪」
    if (state.items.isEmpty()) {
        HomeSkeleton(onOpenSearch)
        return
    }

    val scope = rememberCoroutineScope()
    val refreshing = state.refreshing
    val notice = state.notice

    LaunchedEffect(Unit) { state.loadNews() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .glassPage(),
        contentPadding = PaddingValues(bottom = 34.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { Header(state, onOpenSearch, onOpenDetail, onOpenNews) }

        if (notice != null) {
            paddedItem {
                NoticeBanner(notice) { scope.launch { state.refresh() } }
            }
        }

        // 今日精选
        paddedItem { Reveal(0) { SectionHeader("今日精选", "人工核对来源的长期项目") } }
        item {
            Reveal(1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.featured.take(3)) { item ->
                        FeaturedCard(item) { onOpenDetail(item) }
                    }
                }
            }
        }

        // 分类
        paddedItem { Reveal(2) { SectionHeader("资源分类", "13 个方向，含移植与模拟") } }
        item {
            Reveal(3) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(Category.entries) { category ->
                        CategoryTile(category) { onOpenCategory(category) }
                    }
                }
            }
        }

        // 最新收录
        paddedItem { Reveal(4) { SectionHeader("最新收录", "内置库策展顺序 · 联网后按真实更新时间重排") } }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items.take(10)) { item ->
                    ResourceCard(item) { onOpenDetail(item) }
                }
            }
        }

        // 移植与模拟
        paddedItem { SectionHeader("移植与模拟", "官方移植 vs 网络传闻：把「能玩」和「能装」分清楚") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.inCategory(Category.PORT)) { item ->
                    ResourceCard(item) { onOpenDetail(item) }
                }
            }
        }

        // 最近更新
        paddedItem { SectionHeader("最近更新", "数据来自各项目在 GitHub 的最后推送时间") }
        paddedItem {
            if (state.recentlyUpdated.isEmpty()) {
                HubCard {
                    Text(
                        "还没有真实更新时间。点下面按钮会从 GitHub 拉取，不会用假日期填充。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    PillButton(text = if (refreshing) "获取中…" else "获取最新更新", filled = false) {
                        scope.launch { state.refresh() }
                    }
                }
            } else {
                HubCard(padding = 14.dp) {
                    state.recentlyUpdated.take(4).forEachIndexed { index, item ->
                        ResourceRow(item) { onOpenDetail(item) }
                        if (index < 3) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // GitHub 热门
        paddedItem { SectionHeader("GitHub 热门 iOS 项目", "Star 800+ 且近一年有推送，实时检索") }
        paddedItem {
            if (state.trending.isEmpty()) {
                HubCard {
                    Text(
                        "需要网络才能看到实时热门项目。内置条目本身不依赖这个区块。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    PillButton(text = if (refreshing) "获取中…" else "从 GitHub 获取", filled = true) {
                        scope.launch { state.refresh() }
                    }
                }
            } else {
                HubCard(padding = 14.dp) {
                    state.trending.take(5).forEachIndexed { index, repo ->
                        GitHubRepoRow(repo) {
                            val item = state.items.firstOrNull {
                                it.name.equals(repo.fullName.substringAfter("/"), true)
                            }
                            if (item != null) onOpenDetail(item)
                        }
                        if (index < 4) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // 资讯
        paddedItem {
            SectionHeader("最新资讯", "官方 RSS 原样聚合，不改写标题") {
                Text(
                    "全部",
                    fontSize = 11.sp,
                    color = Brand,
                    modifier = Modifier
                        .clickable { onOpenNews() }
                        .padding(start = 8.dp),
                )
            }
        }
        paddedItem {
            when {
                state.newsLoading && state.news.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(height = 44.dp, corner = 10.dp)
                    SkeletonBlock(height = 44.dp, corner = 10.dp)
                }
                state.news.isEmpty() -> HubCard {
                    Text(
                        "资讯需要联网获取。没有网络时不展示任何编造的新闻。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    PillButton(text = "获取资讯", filled = false) { onOpenNews() }
                }
                else -> HubCard(padding = 12.dp) {
                    state.news.take(3).forEachIndexed { index, item ->
                        HomeNewsRow(item) { onOpenNews() }
                        if (index < 2) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // 本周热门
        paddedItem { SectionHeader("本周热门", "按 GitHub Star 排行的内置条目") }
        paddedItem {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.weeklyPopular.take(6).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            Box(Modifier.weight(1f)) {
                                WeeklyTile(item) { onOpenDetail(item) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // 数据说明
        paddedItem {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(Glyph.Shield, size = 12.dp, tint = MaterialTheme.colorScheme.onSurface, strokeWidth = 1.7.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("数据来源说明", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "所有资源均指向开发者官方页面或 GitHub 原始仓库，App 不托管、不分发未经授权的 IPA。标记「待验证」的字段表示尚未核对，请以原始来源为准。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { BrandFooter() }
    }
}

// MARK: - 头部

@Composable
private fun Header(
    state: AppState,
    onOpenSearch: () -> Unit,
    onOpenDetail: (ResourceItem) -> Unit,
    onOpenNews: () -> Unit,
) {
    Box {
        // 品牌柔光：给头部一点「氛围」，不占布局
        GlowBox(
            color = Brand,
            size = 240.dp,
            alpha = 0.28f,
            modifier = Modifier.offset(x = (-60).dp, y = (-90).dp),
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(size = 42.dp)
                Spacer(Modifier.width(11.dp))
                Column {
                    BrandSignature(titleSize = 24)
                    Text(
                        greeting(state.items.size),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Brand.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .border(1.dp, Brand.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Bell, size = 16.dp, tint = Brand)
                }
            }
            Spacer(Modifier.height(13.dp))
            SearchEntry(onClick = onOpenSearch)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChip("手气不错", glyph = Glyph.Dice) {
                    if (state.items.isNotEmpty()) onOpenDetail(state.items.random())
                }
                QuickChip("资讯", glyph = Glyph.News) { onOpenNews() }
            }
        }
    }
}

private fun greeting(count: Int): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> "早上好，今天有 $count 个资源待发现"
        in 11..13 -> "中午好，看看社区的新动态"
        in 14..18 -> "下午好，一次把生态逛完"
        else -> "晚上好，睡前刷一遍更新"
    }
}

// MARK: - 精选大卡

@Composable
private fun FeaturedCard(item: ResourceItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "featuredScale",
    )
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .width(300.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = Brand.copy(alpha = 0.45f),
                spotColor = BrandDeep.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(BrandBrush)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        // 斜向高光
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(430f, 330f),
                    ),
                ),
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.category.mark(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(item.category.title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
                    Text(
                        item.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("↗", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(11.dp))
            Text(
                item.summary,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassBadge(item.risk.label)
                Spacer(Modifier.width(6.dp))
                GlassBadge(item.compatibility.label)
                Spacer(Modifier.weight(1f))
                Text(item.versionText, fontSize = 9.5f.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun GlassBadge(text: String) {
    Text(
        text,
        fontSize = 9.5f.sp,
        color = Color.White,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(9.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

// MARK: - 网格砖

@Composable
private fun WeeklyTile(item: ResourceItem, onClick: () -> Unit) {
    val tint = item.category.tintColor()
    PressableSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(134.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryMark(category = item.category, size = 30.dp, radius = 9.dp)
                Spacer(Modifier.weight(1f))
                Text("↗", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(9.dp))
            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                item.summary,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                item.stars?.let { "★ ${compact(it)}" } ?: item.category.short,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.stars == null) tint else Amber,
            )
        }
    }
}

// MARK: - 骨架屏

@Composable
private fun HomeSkeleton(onOpenSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassPage()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogo(size = 42.dp)
            Spacer(Modifier.width(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBlock(height = 14.dp, corner = 6.dp, modifier = Modifier.width(120.dp))
                SkeletonBlock(height = 9.dp, corner = 4.dp, modifier = Modifier.width(180.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        SearchEntry(onClick = onOpenSearch)
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(height = 172.dp, corner = 22.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(5) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                )
            }
        }
        SkeletonBlock(height = 96.dp, corner = 18.dp)
        SkeletonBlock(height = 96.dp, corner = 18.dp)
        Text(
            "正在读取内置资源库…",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - 辅助

@Composable
private fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (filled) Brand else Brand.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) Color.White else Brand,
        )
    }
}

@Composable
private fun QuickChip(text: String, glyph: Glyph? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, size = 12.dp, tint = Brand, strokeWidth = 1.7.dp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            fontSize = 10.5f.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HomeNewsRow(item: com.nevercrynow.ioshub.data.NewsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonogramMark(text = "NW", tint = Slate, size = 22.dp, radius = 7.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 11.5f.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.source} · ${item.publishedText}",
                fontSize = 9.5f.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.paddedItem(content: @Composable () -> Unit) {
    item {
        Box(Modifier.padding(horizontal = 20.dp)) {
            content()
        }
    }
}
