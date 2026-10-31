package com.nevercrynow.ioshub.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.LinkRouter
import com.nevercrynow.ioshub.data.Dates
import com.nevercrynow.ioshub.data.GitHubApi
import com.nevercrynow.ioshub.data.ResourceItem

@Composable
fun DetailScreen(state: AppState, itemId: String, onBack: () -> Unit) {
    val item = state.byId(itemId)
    if (item == null) {
        EmptyState(Glyph.Spark, "资源不存在", "这条资源可能已被移除。")
        return
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isFavorite = state.isFavorite(item.id)
    val favoriteScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favoriteScale",
    )

    var readme by remember(itemId) { mutableStateOf<AppState.ReadmePayload?>(null) }
    var readmeLoading by remember(itemId) { mutableStateOf(false) }
    var retryTick by remember(itemId) { mutableStateOf(0) }

    LaunchedEffect(itemId, retryTick) {
        readmeLoading = true
        readme = state.loadReadme(itemId)
        readmeLoading = false
        state.recordVisit(itemId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar(
            title = item.name,
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "复制",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("iOS HUB", item.sourceUrl),
                            )
                            android.widget.Toast
                                .makeText(context, "已复制原始来源链接", android.widget.Toast.LENGTH_SHORT)
                                .show()
                        }
                        .padding(horizontal = 6.dp),
                )
                Text(
                    if (isFavorite) "♥" else "♡",
                    fontSize = 20.sp,
                    color = if (isFavorite) Rose else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = favoriteScale
                            scaleY = favoriteScale
                        }
                        .clickable { state.toggleFavorite(item.id) }
                        .padding(horizontal = 8.dp),
                )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HeroCard(item) }

            item {
                ActionBar(
                    item = item,
                    isFavorite = isFavorite,
                    onFavorite = { state.toggleFavorite(item.id) },
                    onOpen = { safeOpen(uriHandler, it) },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${item.name} · ${item.sourceUrl}")
                        }
                        context.startActivity(Intent.createChooser(intent, "分享资源"))
                    },
                )
            }

            if (item.risk.level >= 2) {
                item { RiskNotice(item) }
            }

            item { SpecCard(item) }
            item { SummaryCard(item) }
            item { TutorialCard(item) { safeOpen(uriHandler, it) } }

            readme?.release?.let { release ->
                item { ReleaseSection(release) { safeOpen(uriHandler, it) } }
            }

            item {
                ReadmeSection(
                    payload = readme,
                    loading = readmeLoading,
                    onRetry = { retryTick += 1 },
                )
            }

            item { SourceCard(item) { safeOpen(uriHandler, it) } }
            item { VerificationCard(item) }
            item { BrandFooter(note = "iOS HUB · by nevercrynow · ${item.category.title}") }
        }

        PrimaryBar(item) { safeOpen(uriHandler, it) }
    }
}

// MARK: - 底部固定主按钮

@Composable
private fun PrimaryBar(item: ResourceItem, onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassPage()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Brand.copy(alpha = 0.5f), spotColor = Brand.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp))
                .background(BrandBrush)
                .clickable { onOpen(item.primaryUrl) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.primaryLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("· ${item.versionText}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.weight(1f))
            Text("↗", fontSize = 13.sp, color = Color.White)
        }
    }
}

private fun safeOpen(handler: UriHandler, raw: String?) {
    if (raw.isNullOrBlank()) return
    // 按当前线路改写（只影响 GitHub 域名），再交给系统浏览器
    runCatching { handler.openUri(LinkRouter.resolve(raw)) }
}

// MARK: - Hero

@Composable
private fun HeroCard(item: ResourceItem) {
    val tint = item.category.tintColor()
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, clip = false, ambientColor = tint.copy(alpha = 0.40f), spotColor = tint.copy(alpha = 0.22f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, tint.copy(alpha = 0.18f), shape),
    ) {
        // 分类色渐变底：让每个详情页有自己的「色调」
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(tint.copy(alpha = 0.16f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(700f, 420f),
                    ),
                ),
        )
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryMark(category = item.category, size = 60.dp, radius = 18.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.developer,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row {
                    Pill(item.category.title, item.category.tintColor())
                    Spacer(Modifier.width(5.dp))
                    Pill(item.compatibility.label, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("版本 ${item.versionText}", fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${item.risk.label}", fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// MARK: - 行动

@Composable
private fun ActionBar(
    item: ResourceItem,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onOpen: (String) -> Unit,
    onShare: () -> Unit,
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(label = "GitHub", enabled = item.githubUrl != null, modifier = Modifier.weight(1f)) { onOpen(item.githubUrl ?: item.sourceUrl) }
            ActionChip(label = "教程", enabled = item.tutorialUrl != null, modifier = Modifier.weight(1f)) { onOpen(item.tutorialUrl ?: item.sourceUrl) }
            ActionChip(label = "分享", enabled = true, modifier = Modifier.weight(1f)) { onShare() }
            ActionChip(label = if (isFavorite) "已收藏" else "收藏", enabled = true, modifier = Modifier.weight(1f), highlight = isFavorite) { onFavorite() }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                highlight -> Rose
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
    }
}

// MARK: - 风险

@Composable
private fun RiskNotice(item: ResourceItem) {
    val tint = if (item.risk.level >= 3) Rose else Amber
    Column(
        Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphIcon(Glyph.Warning, size = 13.dp, tint = tint, strokeWidth = 1.8.dp)
            Spacer(Modifier.width(5.dp))
            Text("安全提示 · ${item.risk.label}", fontSize = 11.5f.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(item.risk.detail, fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            "建议：仅在可恢复设备上操作，先备份；不要在主力机上测试来源不明的 IPA 或漏洞利用工具。",
            fontSize = 10.5f.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - 规格

@Composable
private fun SpecCard(item: ResourceItem) {
    HubCard {
        Text("基本信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecCell("当前版本", item.versionText, Modifier.weight(1f))
            SpecCell("最近更新", Dates.relative(item.lastUpdated) ?: "待验证", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecCell("支持 iOS", item.supportedIosText, Modifier.weight(1f))
            SpecCell("支持设备", item.supportedDevicesText, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecCell("开发者", item.developer, Modifier.weight(1f))
            SpecCell("分类", item.category.title, Modifier.weight(1f))
        }
        if (item.stars != null) {
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("★ ${compact(item.stars)}", fontSize = 10.sp, color = Amber)
                if (item.forks != null) {
                    Text("  ⑂ ${compact(item.forks)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.language != null) {
                    Text("  ${item.language}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text("GitHub 实时", fontSize = 9.5f.sp, color = Mint)
            }
        }
    }
}

@Composable
private fun SpecCell(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Text(title, fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.5f.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// MARK: - 简介

@Composable
private fun SummaryCard(item: ResourceItem) {
    HubCard {
        Text("项目介绍", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text(item.summary, fontSize = 12.sp)
        if (!item.detail.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(item.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.tags.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { tag -> Pill(tag, item.category.tintColor()) }
                    }
                }
            }
        }
    }
}

// MARK: - 教程

@Composable
private fun TutorialCard(item: ResourceItem, onOpen: (String) -> Unit) {
    HubCard {
        Text("使用教程", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        val tutorial = item.tutorialUrl
        if (tutorial != null) {
            Text(
                "打开官方安装 / 使用说明  ↗",
                fontSize = 12.sp,
                color = Brand,
                modifier = Modifier.clickable { onOpen(tutorial) },
            )
        } else {
            Text(
                "该资源暂未提供独立教程。请打开 GitHub 首页的 README，按作者说明操作；不同 iOS 版本的步骤可能不同。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// MARK: - 来源

@Composable
private fun SourceCard(item: ResourceItem, onOpen: (String) -> Unit) {
    HubCard {
        Text("原始来源", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        SourceRow("项目主页", item.sourceUrl, onOpen)
        item.githubUrl?.let { SourceRow("GitHub 仓库", it, onOpen) }
        item.downloadUrl?.let { SourceRow("官方下载", it, onOpen) }
        item.tutorialUrl?.let { SourceRow("使用教程", it, onOpen) }
        Spacer(Modifier.height(6.dp))
        Text(
            "iOS HUB 不托管任何安装包，以上链接均指向开发者或官方平台。",
            fontSize = 9.5f.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SourceRow(label: String, url: String, onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            url.substringAfter("://").substringBefore("/").take(28),
            fontSize = 10.sp,
            color = Brand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("  ↗", fontSize = 10.sp, color = Brand)
    }
}

// MARK: - 核对状态

@Composable
private fun VerificationCard(item: ResourceItem) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brand.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("兼容性 · ${item.compatibility.label}", fontSize = 11.5f.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("核对：${item.lastUpdated?.let { Dates.relative(it) } ?: "尚未核对"}", fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!item.verificationNote.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(item.verificationNote, fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


// MARK: - 最新 Release

@Composable
private fun ReleaseSection(release: GitHubApi.Release, onOpen: (String) -> Unit) {
    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最新发布", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Dates.relative(release.publishedAt)?.let { text ->
                Text(text, fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(release.tagName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Brand)
        val name = release.name
        if (!name.isNullOrBlank() && name != release.tagName) {
            Text(name, fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val body = release.body
        if (!body.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            val blocks = remember(body) { Markdown.parse(body.take(3000), maxBlocks = 40) }
            MarkdownBody(blocks)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "在 GitHub 查看 Release ↗",
            fontSize = 10.5f.sp,
            fontWeight = FontWeight.SemiBold,
            color = Brand,
            modifier = Modifier.clickable { onOpen(release.htmlUrl) },
        )
    }
}

// MARK: - README

@Composable
private fun ReadmeSection(payload: AppState.ReadmePayload?, loading: Boolean, onRetry: () -> Unit) {
    HubCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("项目 README", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (loading) "读取中…" else "GitHub 实时",
                fontSize = 9.5f.sp,
                color = if (loading) MaterialTheme.colorScheme.onSurfaceVariant else Mint,
            )
        }
        Spacer(Modifier.height(8.dp))

        val markdown = payload?.markdown
        when {
            loading && markdown == null -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBlock(height = 12.dp, corner = 6.dp)
                SkeletonBlock(height = 12.dp, corner = 6.dp)
                SkeletonBlock(height = 12.dp, corner = 6.dp)
            }

            markdown.isNullOrBlank() -> {
                Text(
                    "这个仓库没有可展示的 README，或内容无法解析。",
                    fontSize = 10.5f.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "重试",
                    fontSize = 10.5f.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand,
                    modifier = Modifier.clickable(onClick = onRetry),
                )
            }

            else -> {
                val blocks = remember(markdown) { Markdown.parse(markdown) }
                MarkdownBody(blocks)
                if (payload.truncated) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "README 较长，这里只展示前 2 万字符。",
                        fontSize = 9.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
