package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.LinkRouter
import com.nevercrynow.ioshub.data.NewsFeed
import com.nevercrynow.ioshub.data.NewsItem
import kotlinx.coroutines.launch

/** 资讯：官方 RSS 聚合，标题与链接原样保留，不改写、不生成。 */
@Composable
fun NewsScreen(state: AppState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) { state.loadNews() }

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar(
            title = "资讯",
            onBack = onBack,
            trailing = {
                Text(
                    if (state.newsLoading) "刷新中…" else "刷新",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand,
                    modifier = Modifier
                        .clickable { scope.launch { state.loadNews(force = true) } }
                        .padding(8.dp),
                )
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.newsError?.let { message ->
                item { NoticeBanner(message) { scope.launch { state.loadNews(force = true) } } }
            }

            if (state.newsLoading && state.news.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(height = 54.dp, corner = 12.dp)
                        SkeletonBlock(height = 54.dp, corner = 12.dp)
                        SkeletonBlock(height = 54.dp, corner = 12.dp)
                    }
                }
            }

            if (!state.newsLoading && state.news.isEmpty()) {
                item {
                    EmptyState(
                        glyph = Glyph.News,
                        title = "暂无资讯",
                        message = "资讯只在联网时从官方 RSS 拉取。没有网络时不会生成任何替代内容。",
                    )
                }
            }

            if (state.news.isNotEmpty()) {
                item {
                    HubCard(padding = 14.dp) {
                        state.news.forEachIndexed { index, item ->
                            NewsRow(item) { uriHandler.openUri(LinkRouter.resolve(item.url)) }
                            if (index < state.news.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            item { SourcesCard { uriHandler.openUri(LinkRouter.resolve(it)) } }
            item { BrandFooter(note = "资讯 · 官方 RSS 实时抓取 · by nevercrynow") }
        }
    }
}

@Composable
private fun NewsRow(item: NewsItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MonogramMark(text = "NW", tint = Slate, size = 24.dp, radius = 8.dp)
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 12.5f.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                Text(item.source, fontSize = 9.5f.sp, color = Brand)
                Text(" · ", fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.publishedText, fontSize = 9.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourcesCard(onOpen: (String) -> Unit) {
    HubCard {
        Text("信息源", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "只聚合官方与公开社区源，标题与链接原样保留，不改编、不伪造发布时间。",
            fontSize = 10.5f.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        NewsFeed.entries.forEach { feed ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(feed.siteUrl) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonogramMark(text = feed.title.take(1).uppercase(), tint = Slate, size = 24.dp, radius = 8.dp)
                Column(Modifier.weight(1f)) {
                    Text(feed.title, fontSize = 11.5f.sp)
                    Text(
                        feed.note,
                        fontSize = 9.5f.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("↗", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
