package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.Category
import com.nevercrynow.ioshub.data.LinkRouter
import com.nevercrynow.ioshub.data.ResourceItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// MARK: - 顶栏

@Composable
fun HubTopBar(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    GlassTopBar {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Text(
                "←",
                fontSize = 19.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 6.dp),
            )
        }
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 4.dp else 8.dp),
        )
        if (trailing != null) {
            trailing()
        } else {
            Text(
                "by nevercrynow",
                fontSize = 8.5f.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand.copy(alpha = 0.55f),
                letterSpacing = 0.3.sp,
            )
        }
    }
    }
}

// MARK: - 搜索框

@Composable
fun SearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val base = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
    Row(
        modifier = base
            .fillMaxWidth()
            .height(46.dp)
            .glassCapsule(corner = 15.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.Search, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 1.6.dp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(Brand),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (value.isNotEmpty()) {
            Text(
                "✕",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onValueChange("") },
            )
        }
    }
}

// MARK: - 资源 Tab

@Composable
fun ResourcesScreen(state: AppState, onCategory: (Category) -> Unit, onSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar("资源")
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchEntry(placeholder = "搜索资源名称、开发者或 iOS 版本", onClick = onSearch)
            }
            item {
                Text(
                    "${Category.entries.size} 个分类 · ${state.items.size} 个资源",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.usedCategories) { category ->
                CategoryRowFull(category, state.countIn(category)) { onCategory(category) }
            }
            item {
                Text(
                    "分类只收录指向官方仓库 / 开发者主页的条目。标记「待验证」的版本与兼容性尚未核对，请以原始来源为准。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            item { BrandFooter() }
        }
    }
}

@Composable
private fun CategoryRowFull(category: Category, count: Int, onClick: () -> Unit) {
    val tint = category.tintColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryMark(category = category, size = 48.dp, radius = 15.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                category.blurb,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("  ›", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// MARK: - 分类详情

@Composable
fun CategoryScreen(
    state: AppState,
    category: Category,
    onBack: () -> Unit,
    onDetail: (ResourceItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, state.items, category) {
        if (query.isBlank()) state.inCategory(category) else state.search(query, category)
    }

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar(category.title, onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            SearchField(query, "在「${category.title}」中搜索", { query = it })
            Spacer(Modifier.height(6.dp))
            Text(
                "${results.size} 个资源",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (results.isEmpty()) {
            EmptyState(Glyph.Search, "没有匹配的资源", "换个关键词试试，或到「发现」里直接搜索 GitHub（v1.1）。")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            ) {
                items(results) { item ->
                    ResourceRow(item) { onDetail(item) }
                }
                item { BrandFooter(note = "iOS HUB · by nevercrynow · ${category.title}") }
            }
        }
    }
}

// MARK: - 搜索

@Composable
fun SearchScreen(state: AppState, onBack: () -> Unit, onDetail: (ResourceItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val results = remember(query, state.items) {
        if (query.isBlank()) emptyList() else state.search(query)
    }
    val suggestions = listOf("TrollStore", "越狱", "IPA", "侧载", "Theos", "美化")

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar("搜索", onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            SearchField(
                value = query,
                placeholder = "搜索资源、开发者、iOS 版本",
                onValueChange = { query = it },
                focusRequester = focusRequester,
            )
            Spacer(Modifier.height(10.dp))
        }

        when {
            query.isBlank() -> {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    if (state.searchHistory.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("搜索历史", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "清空",
                                fontSize = 10.5f.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { state.clearSearchHistory() },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.searchHistory) { word ->
                                Text(
                                    word,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(9.dp))
                                        .clickable { query = word }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    Text("试试这些", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { word ->
                            Text(
                                word,
                                fontSize = 11.sp,
                                color = Brand,
                                modifier = Modifier
                                    .background(Brand.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                                    .clickable { query = word }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "搜索覆盖内置资源库的名称、开发者、标签、简介与支持版本。",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            results.isEmpty() -> EmptyState(Glyph.Spark, "没有找到「$query」", "试试项目名、开发者名，或 iOS 版本号。")

            else -> {
                Text(
                    "${results.size} 个结果",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
                ) {
                    items(results) { item ->
                        ResourceRow(item) {
                            state.recordSearch(query)
                            onDetail(item)
                        }
                    }
                    item { BrandFooter(note = "搜索 · iOS HUB · by nevercrynow") }
                }
            }
        }
    }
}

// MARK: - 收藏

@Composable
fun FavoritesScreen(state: AppState, onDetail: (ResourceItem) -> Unit) {
    val favorites = state.favoriteItems()

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar(
            title = "收藏",
            trailing = {
                if (state.favorites.isNotEmpty()) {
                    Text(
                        "清空",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { state.clearFavorites() }
                            .padding(8.dp),
                    )
                }
            },
        )
        if (favorites.isEmpty()) {
            EmptyState(Glyph.Heart, "还没有收藏", "在任意资源详情页点右上角的心形，就能把它存到这里。")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(favorites) { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            ResourceRow(item) { onDetail(item) }
                        }
                        Text(
                            "♥",
                            fontSize = 17.sp,
                            color = Rose,
                            modifier = Modifier
                                .clickable { state.toggleFavorite(item.id) }
                                .padding(8.dp),
                        )
                    }
                }

                val recent = state.recentItems()
                if (recent.isNotEmpty()) {
                    item {
                        Text("最近浏览", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    items(recent) { item ->
                        ResourceRow(item) { onDetail(item) }
                    }
                }

                item { BrandFooter() }
            }
        }
    }
}

// MARK: - 我的

@Composable
fun ProfileScreen(state: AppState) {
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar("我的")
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // 品牌 hero：把「谁做的」放在个人页最显眼的位置
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(BrandBrush, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HubMark(size = 52.dp, showsShadow = false)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "iOS HUB",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    "by nevercrynow",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.86f),
                                    letterSpacing = 0.4.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "全生态 iOS 资源中心 · Android 版 v1.0",
                            fontSize = 10.5f.sp,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlyphIcon(Glyph.Dot, size = 10.dp, tint = Color.White.copy(alpha = 0.9f))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "本地用户 · 数据只存在本机，不登录、不上传",
                                fontSize = 10.5f.sp,
                                color = Color.White.copy(alpha = 0.82f),
                            )
                        }
                    }
                }
            }

            item {
                HubCard {
                    Text("数据", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    StatRow("资源条目", "${state.items.size}")
                    StatRow("已收藏", "${state.favorites.size}")
                    StatRow("GitHub 趋势", if (state.trending.isEmpty()) "未获取" else "${state.trending.size} 个项目")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brand, RoundedCornerShape(11.dp))
                            .clickable { scope.launch { state.refresh() } }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (state.refreshing) "正在刷新…" else "立即刷新远端数据",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }

            item { LinkCard() }

            item {
                HubCard {
                    Text("数据来源与免责声明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "所有条目都指向开发者官网、GitHub 仓库或官方发布页。App 不托管、不分发任何 IPA、插件包或漏洞利用工具。越狱与侧载可能违反保修条款并带来安全风险，请在可恢复的非主力设备上测试。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { BrandFooter() }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(label, fontSize = 11.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 11.5f.sp, fontWeight = FontWeight.SemiBold)
    }
}

// MARK: - 网络线路

/**
 * 线路卡片：GitHub 的页面 / API 在部分网络下打不开，
 * 这里让用户选一条镜像，或自己填前缀。
 */
@Composable
private fun LinkCard() {
    val scope = rememberCoroutineScope()
    var custom by remember { mutableStateOf(LinkRouter.customPrefix) }

    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Glyph.Globe, size = 14.dp, tint = Brand)
            Spacer(Modifier.width(6.dp))
            Text("网络线路", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (LinkRouter.current == LinkRouter.Route.AUTO) {
                    val good = LinkRouter.lastGood
                    when {
                        good == null -> "自动 · 待探测"
                        good.isEmpty() -> "自动 · 直连"
                        else -> "自动 · 镜像"
                    }
                } else {
                    LinkRouter.current.label
                },
                fontSize = 10.5f.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "链接打不开时换一条线路：App 只会改写 github.com / api.github.com 的地址，其它站点原样打开。镜像不支持 releases/latest 的跳转，这类链接会自动改成 releases 列表。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LinkRouter.Route.entries.toList()) { route ->
                val selected = LinkRouter.current == route
                Box(
                    Modifier
                        .background(
                            if (selected) Brand else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(9.dp),
                        )
                        .clickable { LinkRouter.select(route) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text(
                        route.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (LinkRouter.current == LinkRouter.Route.CUSTOM) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                if (custom.isBlank()) {
                    Text(
                        "https://your-mirror.example/",
                        fontSize = 11.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = custom,
                    onValueChange = {
                        custom = it
                        LinkRouter.setCustom(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 11.5f.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(Brand),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(Brand.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
                    .clickable {
                        if (!LinkRouter.checking) scope.launch { LinkRouter.healthCheck() }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    if (LinkRouter.checking) "正在测速…" else "测速并自动选最快",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "未认证 GitHub 接口 60 次/小时",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (LinkRouter.results.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LinkRouter.results.forEach { health ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlyphIcon(
                        if (health.ok) Glyph.Check else Glyph.Warning,
                        size = 11.dp,
                        tint = if (health.ok) Mint else Rose,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(health.route.label, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        health.detail,
                        fontSize = 10.5f.sp,
                        color = if (health.ok) MaterialTheme.colorScheme.onSurfaceVariant else Rose,
                    )
                }
            }
        }
    }
}
