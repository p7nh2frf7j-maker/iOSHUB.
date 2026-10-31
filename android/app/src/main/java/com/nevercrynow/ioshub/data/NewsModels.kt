package com.nevercrynow.ioshub.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

/** 资讯条目：只保留标题、来源、摘要、原文链接、发布时间。 */
data class NewsItem(
    val id: String,
    val title: String,
    val source: String,
    val summary: String?,
    val url: String,
    val publishedAt: String?,
    val emoji: String,
) {
    val publishedText: String get() = Dates.relative(publishedAt) ?: "时间待验证"
}

/** 可信信息源。只聚合官方 / 公开社区源，标题与链接原样保留。 */
enum class NewsFeed(
    val id: String,
    val title: String,
    val emoji: String,
    val rssUrl: String?,
    val siteUrl: String,
    val note: String,
) {
    APPLE_DEVELOPER(
        "appleDeveloper", "Apple Developer", "🍎",
        "https://developer.apple.com/news/rss/news.rss",
        "https://developer.apple.com/news/",
        "苹果官方开发者公告，版本与 API 变更的第一手来源。",
    ),
    APPLE_NEWSROOM(
        "appleNewsroom", "Apple Newsroom", "📰",
        "https://www.apple.com/newsroom/rss-feed.rss",
        "https://www.apple.com/newsroom/",
        "苹果官方新闻稿，系统发布节奏与功能说明。",
    ),
    GITHUB_BLOG(
        "githubBlog", "GitHub Blog", "🐙",
        "https://github.blog/feed/",
        "https://github.blog/",
        "GitHub 官方博客，含 Releases / Actions 等平台变更。",
    ),
    REDDIT_JAILBREAK(
        "redditJailbreak", "r/jailbreak", "💬",
        "https://www.reddit.com/r/jailbreak/.rss",
        "https://www.reddit.com/r/jailbreak/",
        "社区讨论聚合，内容未经审核，请以官方说明为准。",
    );

    companion object {
        fun defaults(): List<NewsFeed> = listOf(APPLE_DEVELOPER, APPLE_NEWSROOM, GITHUB_BLOG)
    }
}

data class RssEntry(
    var title: String = "",
    var link: String = "",
    var summary: String = "",
    var published: String = "",
)

/** RSS / Atom 解析：用 Android 自带的 XmlPullParser，零依赖。 */
object RssReader {

    fun parse(xml: String): List<RssEntry> {
        val entries = mutableListOf<RssEntry>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var current: RssEntry? = null
        var text = StringBuilder()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase(Locale.ROOT)
                    if (name == "item" || name == "entry") {
                        current = RssEntry()
                    } else if (current != null) {
                        // Atom 的 <link href="..."/>
                        if (name == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank() && current.link.isBlank()) {
                                current.link = href
                            }
                        }
                        text = StringBuilder()
                    }
                }

                XmlPullParser.TEXT -> {
                    if (current != null) text.append(parser.text)
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase(Locale.ROOT)
                    val value = text.toString().trim()
                    text = StringBuilder()
                    val entry = current
                    if (entry != null) {
                        when (name) {
                            "title" -> if (entry.title.isBlank()) entry.title = clean(value)
                            "link" -> if (entry.link.isBlank()) entry.link = value
                            "description", "summary", "content" ->
                                if (entry.summary.isBlank()) entry.summary = clean(value)
                            "pubdate", "published", "updated" ->
                                if (entry.published.isBlank()) entry.published = value
                            "item", "entry" -> {
                                entries.add(entry)
                                current = null
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return entries
    }

    private fun clean(raw: String): String {
        var value = raw.replace(Regex("<[^>]+>"), "")
        val entities = mapOf(
            "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
            "&apos;" to "'", "&#39;" to "'", "&nbsp;" to " ", "&hellip;" to "…",
        )
        for ((key, replacement) in entities) {
            value = value.replace(key, replacement)
        }
        return value.trim()
    }
}

object NewsLoader {

    /** 单源抓取；失败抛异常，由调用方决定是否提示。 */
    fun load(feed: NewsFeed, limit: Int = 8): List<NewsItem> {
        val rssUrl = feed.rssUrl ?: return emptyList()
        val xml = Http.get(rssUrl, accept = "application/rss+xml, application/xml, text/xml")
        return RssReader.parse(xml)
            .mapNotNull { entry ->
                if (entry.title.isBlank() || entry.link.isBlank()) return@mapNotNull null
                NewsItem(
                    id = entry.link,
                    title = entry.title,
                    source = feed.title,
                    summary = entry.summary.ifBlank { null }?.take(220),
                    url = entry.link,
                    publishedAt = normalizeDate(entry.published),
                    emoji = feed.emoji,
                )
            }
            .sortedByDescending { it.publishedAt ?: "" }
            .take(limit)
    }

    /** 并发拉取多个源，单源失败不影响其它源。 */
    fun loadAll(feeds: List<NewsFeed>, limitPerFeed: Int = 6): Pair<List<NewsItem>, List<String>> {
        val items = mutableListOf<NewsItem>()
        val failures = mutableListOf<String>()
        feeds.forEach { feed ->
            try {
                items.addAll(load(feed, limitPerFeed))
            } catch (e: Exception) {
                failures.add(feed.title)
            }
        }
        return items.sortedByDescending { it.publishedAt ?: "" } to failures
    }

    /** 把 RFC822 / ISO8601 统一成 ISO8601 字符串，方便排序与相对时间显示。 */
    private fun normalizeDate(raw: String): String? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        runCatching { return java.time.OffsetDateTime.parse(trimmed).toInstant().toString() }
        runCatching { return java.time.Instant.parse(trimmed).toString() }
        runCatching {
            val formatter = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            return java.time.ZonedDateTime.parse(trimmed, formatter).toInstant().toString()
        }
        runCatching {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            return java.time.ZonedDateTime.parse(trimmed, formatter).toInstant().toString()
        }
        return null
    }
}
