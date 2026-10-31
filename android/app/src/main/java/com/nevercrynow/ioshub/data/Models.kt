package com.nevercrynow.ioshub.data

import org.json.JSONArray
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 13 个资源分类。与 iOS 版 ResourceCategory 的 id 完全一致，
 * 因此两端可以共用同一份 seed-resources.json。
 */
enum class Category(
    val id: String,
    val title: String,
    val short: String,
    val blurb: String,
    val emoji: String,
    val tint: Long,
) {
    JAILBREAK("jailbreak", "越狱工具", "越狱", "无需电脑或半自动的越狱方案", "🔓", 0xFF5466FA),
    TROLLSTORE("trollstore", "巨魔商店", "巨魔", "永久签名安装，不越狱也能装", "📦", 0xFF9E6BFA),
    IPA("ipa", "IPA 应用", "IPA", "侧载、备份与安装工具", "📥", 0xFF38C7EB),
    SIDELOAD("sideload", "侧载工具", "侧载", "电脑端 / 移动端签名安装", "📲", 0xFF29C299),
    TWEAK("tweak", "越狱插件", "插件", "注入类插件与运行时框架", "🐜", 0xFFF25C75),
    REPO("repo", "软件源", "软件源", "Cydia / Sileo / Zebra 软件源", "🗂", 0xFFF7AD38),
    OPENSOURCE("openSource", "开源项目", "开源", "可学习、可复用的 iOS 开源代码", "🧩", 0xFF6B7894),
    SECURITY("security", "漏洞研究", "漏洞", "系统公告、漏洞分析与研究资料", "🛡", 0xFFD94D52),
    DEVTOOLS("devTools", "开发工具", "开发", "构建、调试与设备通信工具", "🛠", 0xFF4D8CF2),
    THEMING("theming", "系统美化", "美化", "图标、状态栏与系统观感定制", "🎨", 0xFFB866E6),
    UTILITIES("utilities", "实用工具", "工具", "日常效率与设备管理", "🔧", 0xFF38A8A1),
    PORT("port", "移植与模拟", "移植", "手机跑主机 / PC 游戏的现实与边界", "🎮", 0xFFFF6B4A),
    NEWS("news", "资讯来源", "资讯", "官方与社区的信息源", "📰", 0xFF73737F);

    companion object {
        fun of(raw: String): Category = entries.firstOrNull { it.id.equals(raw, true) } ?: UTILITIES
    }
}

enum class Risk(val label: String, val level: Int, val emoji: String) {
    OFFICIAL("官方来源", 0, "✅"),
    COMMUNITY("社区来源", 1, "👥"),
    CAUTION("谨慎使用", 2, "⚠️"),
    HIGH("高风险", 3, "⛔");

    val detail: String
        get() = when (this) {
            OFFICIAL -> "该资源由官方团队或开发者本人维护，来源可直接核对。"
            COMMUNITY -> "来自社区维护者，代码质量与更新节奏需要自行判断。"
            CAUTION -> "该工具会修改系统文件或依赖系统漏洞，可能影响稳定性与安全策略。"
            HIGH -> "来源无法完整追溯或需要绕过系统保护，请仅在可丢弃的设备上尝试。"
        }

    companion object {
        fun of(raw: String): Risk = entries.firstOrNull { it.name.equals(raw, true) } ?: COMMUNITY
    }
}

enum class Compatibility(val label: String) {
    VERIFIED("已验证"),
    PARTIAL("部分支持"),
    PENDING("待验证"),
    UNSUPPORTED("不支持");

    companion object {
        fun of(raw: String): Compatibility = entries.firstOrNull { it.name.equals(raw, true) } ?: PENDING
    }
}

data class ResourceItem(
    val id: String,
    val name: String,
    val category: Category,
    val summary: String,
    val detail: String?,
    val developer: String,
    val githubUrl: String?,
    val downloadUrl: String?,
    val sourceUrl: String,
    val tutorialUrl: String?,
    val supportedIos: List<String>,
    val supportedDevices: List<String>,
    val compatibility: Compatibility,
    val verificationNote: String?,
    val risk: Risk,
    val tags: List<String>,
    val isFeatured: Boolean,
    val version: String? = null,
    val lastUpdated: String? = null,
    // 运行期由 GitHub API 填充
    val stars: Int? = null,
    val forks: Int? = null,
    val language: String? = null,
) {
    val versionText: String get() = version?.takeIf { it.isNotBlank() } ?: "待验证"

    val supportedIosText: String get() = if (supportedIos.isEmpty()) "待验证" else supportedIos.joinToString(" / ")

    val supportedDevicesText: String get() = if (supportedDevices.isEmpty()) "待验证" else supportedDevices.joinToString("、")

    val primaryUrl: String get() = downloadUrl ?: githubUrl ?: sourceUrl

    val primaryLabel: String
        get() = when {
            downloadUrl != null -> "官方下载"
            githubUrl != null -> "打开 GitHub"
            else -> "查看原始来源"
        }

    val searchText: String by lazy {
        buildString {
            append(name).append(' ')
            append(developer).append(' ')
            append(summary).append(' ')
            append(category.title).append(' ')
            append(version ?: "").append(' ')
            append(tags.joinToString(" ")).append(' ')
            append(supportedIos.joinToString(" ")).append(' ')
            append(supportedDevices.joinToString(" "))
        }.lowercase()
    }
}

object Dates {
    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")

    fun relative(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val instant: Instant = try {
            OffsetDateTime.parse(iso).toInstant()
        } catch (e: Exception) {
            try {
                Instant.parse(iso)
            } catch (e2: Exception) {
                return null
            }
        }
        val minutes = Duration.between(instant, Instant.now()).toMinutes()
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "$minutes 分钟前"
            minutes < 60 * 24 -> "${minutes / 60} 小时前"
            minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)} 天前"
            else -> dayFormatter.withZone(ZoneId.systemDefault()).format(instant)
        }
    }
}

object SeedParser {
    fun parse(json: String): List<ResourceItem> {
        val array = JSONArray(json)
        val result = ArrayList<ResourceItem>(array.length())
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            result += ResourceItem(
                id = id,
                name = obj.optString("name"),
                category = Category.of(obj.optString("category")),
                summary = obj.optString("summary"),
                detail = obj.optString("detail").ifBlank { null },
                developer = obj.optString("developer"),
                githubUrl = obj.optString("githubURL").ifBlank { null },
                downloadUrl = obj.optString("downloadURL").ifBlank { null },
                sourceUrl = obj.optString("sourceURL"),
                tutorialUrl = obj.optString("tutorialURL").ifBlank { null },
                supportedIos = obj.optJSONArray("supportedIOS").toStringList(),
                supportedDevices = obj.optJSONArray("supportedDevices").toStringList(),
                compatibility = Compatibility.of(obj.optString("compatibility")),
                verificationNote = obj.optString("verificationNote").ifBlank { null },
                risk = Risk.of(obj.optString("risk")),
                tags = obj.optJSONArray("tags").toStringList(),
                isFeatured = obj.optBoolean("isFeatured"),
            )
        }
        return result
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }
}
