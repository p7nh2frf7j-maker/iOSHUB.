package com.nevercrynow.ioshub.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 线路：把 github.com / api.github.com 这类"国内时通时不通"的地址，
 * 按用户选的规则改写成可访问的镜像地址。
 *
 * 设计原则：
 * - 只改写 GitHub 域名，其它站点（Apple、开发者官网）原样打开，不做中间人。
 * - "自动" = 启动时探测一次（直连 vs 镜像），把结果记在 lastGood，之后零延时直接用。
 * - 任何时候都能在「我的 → 网络线路」里手动锁定某条线路或填自己的前缀。
 */
object LinkRouter {

    enum class Route(
        val id: String,
        val label: String,
        val prefix: String?,
        val note: String,
    ) {
        AUTO("auto", "自动", null, "探测一次，谁通走谁"),
        DIRECT("direct", "直连", null, "不经过任何镜像"),
        GHPROXY("ghproxy", "gh-proxy", "https://gh-proxy.com/", "最稳：页面 / 文件 / API 都代理"),
        GHFAST("ghfast", "ghfast", "https://ghfast.top/", "备用线路，部分网络不通"),
        GITMIRROR("gitmirror", "gitmirror", "https://hub.gitmirror.com/", "备用线路，偏文件下载"),
        CUSTOM("custom", "自定义", "", "前缀 + 原始地址"),
    }

    /** 命中这些域名才会被改写。 */
    private val PROXYABLE = setOf(
        "github.com",
        "www.github.com",
        "api.github.com",
        "raw.githubusercontent.com",
        "gist.githubusercontent.com",
        "objects.githubusercontent.com",
        "codeload.github.com",
        "avatars.githubusercontent.com",
    )

    data class Health(val route: Route, val ok: Boolean, val millis: Long, val detail: String)

    private var prefs: android.content.SharedPreferences? = null

    var current by mutableStateOf(Route.AUTO)
        private set

    var customPrefix by mutableStateOf("")
        private set

    /** 探测得到/记住的可用前缀（"" 表示直连可用）。 */
    var lastGood by mutableStateOf<String?>(null)
        private set

    var checking by mutableStateOf(false)
        private set

    var results by mutableStateOf<List<Health>>(emptyList())
        private set

    var checkedAt by mutableStateOf(0L)
        private set

    fun attach(context: Context) {
        if (prefs != null) return
        val store = context.getSharedPreferences("ioshub", Context.MODE_PRIVATE)
        prefs = store
        current = Route.entries.firstOrNull { it.id == store.getString("link.route", "auto") } ?: Route.AUTO
        customPrefix = store.getString("link.custom", "") ?: ""
        lastGood = store.getString("link.lastGood", null)
    }

    fun select(route: Route) {
        current = route
        prefs?.edit()?.putString("link.route", route.id)?.apply()
    }

    fun setCustom(prefix: String) {
        customPrefix = prefix.trim()
        prefs?.edit()?.putString("link.custom", customPrefix)?.apply()
        if (current == Route.CUSTOM) select(Route.CUSTOM)
    }

    // MARK: - 地址改写

    private fun hostOf(raw: String): String? = runCatching {
        URL(raw).host.lowercase()
    }.getOrNull()

    fun isProxyable(raw: String): Boolean {
        val host = hostOf(raw) ?: return false
        return host in PROXYABLE
    }

    private fun prefixOf(route: Route): String? = when (route) {
        Route.AUTO -> lastGood
        Route.DIRECT -> ""
        Route.GHPROXY, Route.GHFAST, Route.GITMIRROR -> route.prefix
        Route.CUSTOM -> customPrefix.ifBlank { null }
    }?.let { if (it.isEmpty()) "" else if (it.endsWith("/")) it else "$it/" }

    /** 把地址改写成"当前应该打开的那个"。同步、零网络，供点击时立即调用。 */
    fun resolve(raw: String): String {
        if (raw.isBlank()) return raw
        if (!isProxyable(raw)) return raw
        val prefix = prefixOf(current) ?: return raw
        if (prefix.isEmpty()) return raw
        if (raw.startsWith(prefix)) return raw
        return prefix + normalizeForMirror(raw)
    }

    /**
     * 镜像对「会 302 跳到 github.com」的地址会返回 403，
     * 例如 /releases/latest 和 /releases/tag/x.x。
     * 统一改成 releases 列表页，用户仍然能点到具体版本。
     */
    private fun normalizeForMirror(raw: String): String {
        val latest = Regex("/releases/latest/?$")
        val tag = Regex("/releases/tag/[^/]+/?$")
        return when {
            latest.containsMatchIn(raw) -> raw.replace(latest, "/releases")
            tag.containsMatchIn(raw) -> raw.replace(tag, "/releases")
            else -> raw
        }
    }

    /** GitHub API 的候选前缀，按优先级排列（先当前线路，再直连，再镜像）。 */
    fun apiPrefixes(): List<String> {
        val chosen = prefixOf(current)
        val ordered = buildList {
            if (chosen != null) add(chosen)
            add("")
            Route.GHPROXY.prefix?.let { add(it) }
            Route.GHFAST.prefix?.let { add(it) }
            Route.GITMIRROR.prefix?.let { add(it) }
            if (customPrefix.isNotBlank()) add(customPrefix)
        }
        return ordered.distinct()
    }

    fun rememberGood(prefix: String) {
        if (lastGood == prefix) return
        lastGood = prefix
        prefs?.edit()?.putString("link.lastGood", prefix)?.apply()
    }

    // MARK: - 探测

    private fun probeOnce(url: String, timeoutMs: Int = 5000): Pair<Boolean, Long> {
        val started = System.currentTimeMillis()
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "iOSHUB-Android/1.0")
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                val code = connection.responseCode
                connection.inputStream?.close()
                val ok = code in 200..399
                ok to (System.currentTimeMillis() - started)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            false to (System.currentTimeMillis() - started)
        }
    }

    /** 逐个线路探测（并行），结果写进 [results]；同时更新 lastGood。 */
    suspend fun healthCheck(): List<Health> = withContext(Dispatchers.IO) {
        checking = true
        val targets = Route.entries.filter { it != Route.AUTO && (it != Route.CUSTOM || customPrefix.isNotBlank()) }
        val list = coroutineScope {
            targets.map { route ->
                async {
                    val prefix = when (route) {
                        Route.DIRECT -> ""
                        Route.CUSTOM -> if (customPrefix.endsWith("/")) customPrefix else "$customPrefix/"
                        else -> route.prefix ?: ""
                    }
                    val target = "$prefix" + "https://api.github.com/rate_limit"
                    val (ok, millis) = probeOnce(target)
                    Health(
                        route = route,
                        ok = ok,
                        millis = millis,
                        detail = when {
                            ok -> "${millis} ms"
                            millis >= 5000 -> "超时"
                            else -> "连接失败"
                        },
                    )
                }
            }.awaitAll()
        }
        results = list
        checkedAt = System.currentTimeMillis()
        checking = false
        // 自动模式下，把最快可用的一条记下来（直连优先，省流量）
        if (current == Route.AUTO) {
            val ordered = list.filter { it.ok }.sortedWith(
                compareBy({ it.route != Route.DIRECT }, { it.millis }),
            )
            val best = ordered.firstOrNull()?.route
            when (best) {
                null -> Unit
                Route.DIRECT -> rememberGood("")
                else -> rememberGood(best.prefix ?: "")
            }
        }
        list
    }

    /** 启动时的静默探测：只探「直连能否访问 api.github.com」，不通就切到 gh-proxy。 */
    suspend fun autoDetect() {
        if (current != Route.AUTO) return
        val (ok, _) = withContext(Dispatchers.IO) { probeOnce("https://api.github.com/rate_limit", 4000) }
        if (ok) {
            rememberGood("")
        } else {
            val (mirrorOk, _) = withContext(Dispatchers.IO) {
                probeOnce("https://gh-proxy.com/https://api.github.com/rate_limit", 5000)
            }
            if (mirrorOk) rememberGood("https://gh-proxy.com/")
        }
    }
}
