//
//  NewsFeedLoader.swift
//  iOSHUB
//
//  RSS / Atom 解析（Phase 4 能力，Phase 1 已可用）。
//  只保留标题、链接、摘要、发布时间；不重写、不臆测内容。
//  解析失败时上层降级为「打开原始信息源」。
//

import Foundation

struct RSSEntry: Sendable {
    var title: String = ""
    var link: String = ""
    var summary: String = ""
    var published: String = ""
}

final class RSSParser: NSObject, XMLParserDelegate {
    private(set) var entries: [RSSEntry] = []
    private var current: RSSEntry?
    private var buffer: String = ""
    private var currentElement: String = ""

    func parse(_ data: Data) -> [RSSEntry] {
        entries = []
        current = nil
        let parser = XMLParser(data: data)
        parser.delegate = self
        parser.shouldResolveExternalEntities = false
        parser.parse()
        return entries
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        currentElement = elementName.lowercased()
        buffer = ""

        if currentElement == "item" || currentElement == "entry" {
            current = RSSEntry()
            return
        }

        // Atom: <link href="..."/>
        if currentElement == "link",
           let href = attributeDict["href"],
           var entry = current,
           entry.link.isEmpty {
            entry.link = href
            current = entry
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        buffer += string
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        let name = elementName.lowercased()
        let value = buffer.trimmingCharacters(in: .whitespacesAndNewlines)
        buffer = ""

        guard var entry = current else { return }

        switch name {
        case "title":
            if entry.title.isEmpty { entry.title = Self.stripHTML(value) }
        case "link":
            if entry.link.isEmpty { entry.link = value }
        case "description", "summary", "content":
            if entry.summary.isEmpty { entry.summary = Self.stripHTML(value) }
        case "pubdate", "published", "updated", "date":
            if entry.published.isEmpty { entry.published = value }
        case "item", "entry":
            entries.append(entry)
            current = nil
            return
        default:
            break
        }

        current = entry
    }

    private static func stripHTML(_ raw: String) -> String {
        var text = raw.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        let entities = [
            "&amp;": "&", "&lt;": "<", "&gt;": ">",
            "&quot;": "\"", "&apos;": "'", "&#39;": "'",
            "&nbsp;": " ", "&hellip;": "…", "&mdash;": "—"
        ]
        for (key, value) in entities {
            text = text.replacingOccurrences(of: key, with: value)
        }
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

enum NewsLoader {
    /// 单个信息源的抓取结果。用自定义枚举而不是 Result<_, Error>，
    /// 是为了让子任务结果满足 Sendable 约束。
    private enum FeedOutcome: Sendable {
        case success([NewsItem])
        case failure(NewsFeed)
    }

    /// 拉取单个信息源。返回值已按时间倒序，最多 `limit` 条。
    static func load(
        feed: NewsFeed,
        limit: Int = 8,
        client: HTTPClient = HTTPClient()
    ) async throws -> [NewsItem] {
        guard let url = feed.feedURL else { throw APIError.invalidURL }
        let data = try await client.get(url, headers: ["Accept": "application/rss+xml, application/xml, text/xml"])
        let entries = RSSParser().parse(data)

        let items: [NewsItem] = entries.compactMap { entry in
            guard !entry.title.isEmpty, let link = URL(string: entry.link) else { return nil }
            return NewsItem(
                id: entry.link,
                title: entry.title,
                source: feed.title,
                summary: entry.summary.isEmpty ? nil : entry.summary,
                url: link,
                publishedAt: DateCoding.parse(entry.published),
                feed: feed
            )
        }

        return Array(
            items
                .sorted { ($0.publishedAt ?? .distantPast) > ($1.publishedAt ?? .distantPast) }
                .prefix(limit)
        )
    }

    /// 并发拉取多个源；单个源失败不影响其它源。
    static func loadAll(
        feeds: [NewsFeed] = [.appleDeveloper, .appleSecurity, .githubBlog],
        limitPerFeed: Int = 6,
        client: HTTPClient = HTTPClient()
    ) async -> (items: [NewsItem], failures: [String]) {
        var collected: [NewsItem] = []
        var failures: [String] = []

        await withTaskGroup(of: FeedOutcome.self) { group in
            for feed in feeds {
                group.addTask {
                    do {
                        let items = try await load(feed: feed, limit: limitPerFeed, client: client)
                        return FeedOutcome.success(items)
                    } catch {
                        return FeedOutcome.failure(feed)
                    }
                }
            }

            for await outcome in group {
                switch outcome {
                case .success(let items):
                    collected.append(contentsOf: items)
                case .failure(let feed):
                    failures.append(feed.title)
                }
            }
        }

        collected.sort { ($0.publishedAt ?? .distantPast) > ($1.publishedAt ?? .distantPast) }
        return (collected, failures)
    }
}
