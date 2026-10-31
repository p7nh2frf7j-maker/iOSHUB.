package com.nevercrynow.ioshub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevercrynow.ioshub.data.AppState
import com.nevercrynow.ioshub.data.LinkRouter
import com.nevercrynow.ioshub.data.GitHubApi
import kotlinx.coroutines.delay

/** 发现：直接检索 GitHub 上的 iOS 生态项目。 */
@Composable
fun DiscoverScreen(state: AppState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GitHubApi.Repo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    val keywords = listOf("trollstore", "jailbreak", "ipa sideload", "theos tweak", "swiftui", "ios exploit")

    LaunchedEffect(query) {
        val keyword = query.trim()
        if (keyword.length < 2) {
            results = emptyList()
            error = null
            loading = false
            return@LaunchedEffect
        }
        delay(400)
        loading = true
        error = null
        results = try {
            GitHubApi.search(keyword, limit = 20)
        } catch (e: Exception) {
            error = e.message ?: "GitHub 搜索失败，请稍后重试。"
            emptyList()
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .glassPage(),
    ) {
        HubTopBar("发现")

        Column(Modifier.padding(horizontal = 20.dp)) {
            SearchField(
                value = query,
                placeholder = "搜索 GitHub 项目，如 trollstore",
                onValueChange = { query = it },
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(keywords) { word ->
                    Text(
                        word,
                        fontSize = 10.5f.sp,
                        color = Brand,
                        modifier = Modifier
                            .background(Brand.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { query = word }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (query.trim().length < 2) "输入关键词后自动搜索（不是实时输入即搜，避免烧配额）"
                else if (loading) "正在查询 GitHub…"
                else "${results.size} 个结果 · 按 Star 排序",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        error?.let { message ->
            Row(Modifier.padding(horizontal = 20.dp)) {
                NoticeBanner(message) { query = query.trim() }
            }
        }

        when {
            loading && results.isEmpty() -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(height = 46.dp, corner = 12.dp)
                SkeletonBlock(height = 46.dp, corner = 12.dp)
                SkeletonBlock(height = 46.dp, corner = 12.dp)
            }

            results.isEmpty() -> Column(Modifier.padding(20.dp)) {
                if (state.trending.isEmpty()) {
                    EmptyState(Glyph.Search, "搜点什么", "输入项目名或关键词，直接检索 GitHub 上的 iOS 项目。")
                } else {
                    Text("当前热门", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    state.trending.take(5).forEach { repo ->
                        GitHubRepoRow(repo) { uriHandler.openUri(LinkRouter.resolve(repo.htmlUrl)) }
                    }
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            ) {
                items(results) { repo ->
                    GitHubRepoRow(repo) { uriHandler.openUri(LinkRouter.resolve(repo.htmlUrl)) }
                }
                item { BrandFooter(note = "发现 · GitHub 实时搜索 · by nevercrynow") }
            }
        }
    }
}
