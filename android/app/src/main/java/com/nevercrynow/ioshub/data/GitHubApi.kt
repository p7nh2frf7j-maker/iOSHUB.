package com.nevercrynow.ioshub.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * GitHub 公开读接口。未认证每小时 60 次，够轻量刷新用。
 * 用 HttpURLConnection + org.json，避免额外依赖（减少构建失败点）。
 */
object GitHubApi {

    data class Repo(
        val fullName: String,
        val description: String?,
        val stars: Int,
        val forks: Int,
        val language: String?,
        val pushedAt: String?,
        val htmlUrl: String,
    )

    data class Release(
        val tagName: String,
        val name: String?,
        val body: String?,
        val publishedAt: String?,
        val htmlUrl: String,
    )

    private const val API_HOST = "https://api.github.com"
    private const val UA = "iOSHUB-Android/1.0"

    suspend fun trending(limit: Int = 12): List<Repo> = withContext(Dispatchers.IO) {
        val since = LocalDate.now().minusMonths(12).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val query = "topic:ios stars:>800 pushed:>$since"
        val path = "/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = request(path)
        val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList<Repo>()
        (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.toRepo()
        }
    }

    suspend fun repo(owner: String, name: String): Repo? = withContext(Dispatchers.IO) {
        try {
            JSONObject(request("/repos/$owner/$name")).toRepo()
        } catch (e: Exception) {
            null
        }
    }

    /** 关键词搜索（"发现"页用）。 */
    suspend fun search(query: String, limit: Int = 20): List<Repo> = withContext(Dispatchers.IO) {
        val path = "/search/repositories?q=" + URLEncoder.encode(query, "UTF-8") +
            "&sort=stars&order=desc&per_page=$limit"
        val body = request(path)
        val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList<Repo>()
        (0 until items.length()).mapNotNull { index ->
            items.optJSONObject(index)?.toRepo()
        }
    }

    /** 最新 Release；没有正式 Release 的仓库返回 null（属正常情况）。 */
    suspend fun latestRelease(owner: String, name: String): Release? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(request("/repos/$owner/$name/releases/latest"))
            Release(
                tagName = json.optString("tag_name"),
                name = json.optString("name").ifBlank { null },
                body = json.optString("body").ifBlank { null },
                publishedAt = json.optString("published_at").ifBlank { null },
                htmlUrl = json.optString("html_url"),
            )
        } catch (e: Exception) {
            null
        }
    }

    /** README 原文（base64 解码后返回 Markdown 文本）。 */
    suspend fun readme(owner: String, name: String): String? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(request("/repos/$owner/$name/readme"))
            val content = json.optString("content").replace("\n", "")
            val bytes = Base64.decode(content, Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun slug(url: String): Pair<String, String>? {
        val cleaned = url.substringAfter("github.com/", "").trim('/')
        val parts = cleaned.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return parts[0] to parts[1]
    }

    private fun JSONObject.toRepo() = Repo(
        fullName = optString("full_name"),
        description = optString("description").ifBlank { null },
        stars = optInt("stargazers_count"),
        forks = optInt("forks_count"),
        language = optString("language").ifBlank { null },
        pushedAt = optString("pushed_at").ifBlank { null },
        htmlUrl = optString("html_url"),
    )

    /**
     * 带线路回退的请求：按「当前线路 → 直连 → 镜像」顺序试，
     * 第一个 200 就返回，并把这条线路记下来给「自动」用。
     */
    private fun request(path: String): String {
        var lastError: Exception? = null
        for (prefix in LinkRouter.apiPrefixes()) {
            val url = if (path.startsWith("http")) path else prefix + API_HOST + path
            try {
                val body = httpGet(url)
                if (LinkRouter.current == LinkRouter.Route.AUTO) LinkRouter.rememberGood(prefix)
                return body
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("网络不可用，请到「我的 → 网络线路」换一条线路试试。")
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", UA)
            val code = connection.responseCode
            if (code !in 200..299) {
                val message = if (code == 403 || code == 429) {
                    "GitHub 请求超出频率限制，稍后再试。"
                } else {
                    "GitHub 返回 $code"
                }
                throw IllegalStateException(message)
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
