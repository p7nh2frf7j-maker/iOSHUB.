package com.nevercrynow.ioshub.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 全局状态：资源目录、GitHub 趋势、收藏、刷新状态。
 * 故意保持简单（没有 ViewModel / DI / Room），减少新手的构建负担。
 */
class AppState(private val context: Context) {

    var items by mutableStateOf<List<ResourceItem>>(emptyList())
        private set

    var trending by mutableStateOf<List<GitHubApi.Repo>>(emptyList())
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var refreshing by mutableStateOf(false)
        private set

    var loaded by mutableStateOf(false)
        private set

    val favorites = mutableStateListOf<String>()

    private val prefs = context.getSharedPreferences("ioshub", Context.MODE_PRIVATE)

    var news by mutableStateOf<List<NewsItem>>(emptyList())
        private set

    var newsLoading by mutableStateOf(false)
        private set

    var newsError by mutableStateOf<String?>(null)
        private set

    /** 搜索历史（最近 8 条，换行分隔存在 SharedPreferences）。 */
    val searchHistory = mutableStateListOf<String>()

    /** 最近浏览（最近 10 条资源 id）。 */
    val recentIds = mutableStateListOf<String>()

    private val readmeCache = mutableMapOf<String, ReadmePayload>()

    data class ReadmePayload(
        val markdown: String?,
        val release: GitHubApi.Release?,
        val truncated: Boolean,
    )

    init {
        favorites.addAll(prefs.getStringSet("favorites", emptySet()) ?: emptySet())
        searchHistory.addAll(splitStored(prefs.getString(KEY_SEARCH_HISTORY, "")))
        recentIds.addAll(splitStored(prefs.getString(KEY_RECENT_IDS, "")))
    }

    private fun splitStored(raw: String?): List<String> =
        (raw ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    // MARK: 加载

    suspend fun bootstrap() {
        if (loaded) return
        val json = withContext(Dispatchers.IO) {
            context.assets.open("seed-resources.json").bufferedReader().use { it.readText() }
        }
        items = withContext(Dispatchers.Default) { SeedParser.parse(json) }
        loaded = true
    }

    // MARK: 派生

    val featured: List<ResourceItem> get() = items.filter { it.isFeatured }

    val recentlyUpdated: List<ResourceItem>
        get() = items.filter { it.lastUpdated != null }.sortedByDescending { it.lastUpdated }

    val weeklyPopular: List<ResourceItem>
        get() = items.filter { it.stars != null }
            .sortedByDescending { it.stars }
            .ifEmpty { featured.take(6) }

    val usedCategories: List<Category> get() = Category.entries.filter { category -> items.any { it.category == category } }

    fun inCategory(category: Category): List<ResourceItem> = items.filter { it.category == category }

    fun countIn(category: Category): Int = items.count { it.category == category }

    fun byId(id: String): ResourceItem? = items.firstOrNull { it.id == id }

    fun search(query: String, category: Category? = null): List<ResourceItem> {
        val tokens = query.lowercase()
            .split(' ', '，', ',', '、', '\t')
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val pool = if (category == null) items else items.filter { it.category == category }
        return pool
            .filter { item -> tokens.all { item.searchText.contains(it) } }
            .sortedByDescending { item ->
                val nameHit = if (tokens.any { item.name.lowercase().contains(it) }) 1 else 0
                nameHit * 100_000 + (item.stars ?: 0)
            }
    }

    // MARK: 收藏

    fun isFavorite(id: String): Boolean = favorites.contains(id)

    fun toggleFavorite(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
    }

    fun favoriteItems(): List<ResourceItem> = favorites.mapNotNull { byId(it) }

    // MARK: 刷新

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        notice = null
        try {
            try {
                trending = GitHubApi.trending(12)
            } catch (e: Exception) {
                notice = e.message ?: "GitHub 数据获取失败，请稍后重试。"
            }

            val updated = items.toMutableList()
            var requestCount = 0
            for (index in updated.indices) {
                if (requestCount >= 12) break
                val url = updated[index].githubUrl ?: continue
                val slug = GitHubApi.slug(url) ?: continue
                requestCount += 1
                val repo = try {
                    GitHubApi.repo(slug.first, slug.second)
                } catch (e: Exception) {
                    null
                }
                if (repo != null) {
                    updated[index] = updated[index].copy(
                        stars = repo.stars,
                        forks = repo.forks,
                        language = repo.language,
                        lastUpdated = repo.pushedAt,
                    )
                }
            }
            items = updated
        } finally {
            refreshing = false
        }
    }

    // MARK: 资讯

    suspend fun loadNews(force: Boolean = false) {
        if (newsLoading) return
        if (!force && news.isNotEmpty()) return
        newsLoading = true
        newsError = null
        try {
            val result = withContext(Dispatchers.IO) { NewsLoader.loadAll(NewsFeed.defaults()) }
            news = result.first
            newsError = when {
                result.first.isEmpty() -> "资讯源暂时无法访问（需要网络）。"
                result.second.isNotEmpty() -> "部分信息源不可用：" + result.second.joinToString("、")
                else -> null
            }
        } catch (e: Exception) {
            newsError = e.message ?: "资讯加载失败，请稍后再试。"
        } finally {
            newsLoading = false
        }
    }

    // MARK: README / Release

    suspend fun loadReadme(itemId: String): ReadmePayload {
        readmeCache[itemId]?.let { return it }
        val item = byId(itemId)
        val slug = item?.githubUrl?.let { GitHubApi.slug(it) }
        if (item == null || slug == null) {
            val empty = ReadmePayload(null, null, false)
            readmeCache[itemId] = empty
            return empty
        }
        val markdown = GitHubApi.readme(slug.first, slug.second)
        val release = GitHubApi.latestRelease(slug.first, slug.second)
        val payload = ReadmePayload(
            markdown = markdown?.take(20_000),
            release = release,
            truncated = (markdown?.length ?: 0) > 20_000,
        )
        readmeCache[itemId] = payload
        return payload
    }

    // MARK: 搜索历史 / 最近浏览

    fun recordSearch(query: String) {
        val text = query.trim()
        if (text.length < 2) return
        searchHistory.remove(text)
        searchHistory.add(0, text)
        while (searchHistory.size > 8) {
            searchHistory.removeAt(searchHistory.lastIndex)
        }
        prefs.edit().putString(KEY_SEARCH_HISTORY, searchHistory.joinToString("\n")).apply()
    }

    fun clearSearchHistory() {
        searchHistory.clear()
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    fun recordVisit(itemId: String) {
        recentIds.remove(itemId)
        recentIds.add(0, itemId)
        while (recentIds.size > 10) {
            recentIds.removeAt(recentIds.lastIndex)
        }
        prefs.edit().putString(KEY_RECENT_IDS, recentIds.joinToString("\n")).apply()
    }

    fun recentItems(): List<ResourceItem> = recentIds.mapNotNull { byId(it) }

    fun clearFavorites() {
        favorites.clear()
        prefs.edit().putStringSet("favorites", emptySet()).apply()
    }

    private companion object {
        const val KEY_SEARCH_HISTORY = "searchHistory"
        const val KEY_RECENT_IDS = "recentIds"
    }
}
