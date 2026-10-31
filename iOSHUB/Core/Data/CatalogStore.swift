//
//  CatalogStore.swift
//  iOSHUB
//
//  资源目录的单一数据源（@Observable）。
//  职责：加载内置种子数据 → 派生首页各区块 → 联网增量刷新（GitHub / RSS）。
//  设计原则：网络永远是「增强」，断网时 App 仍完整可用。
//

import Foundation
import Observation

// MARK: - 排序方式

enum ResourceSort: String, CaseIterable, Identifiable, Sendable {
    case recommended
    case stars
    case updated
    case name

    var id: String { rawValue }

    var title: String {
        switch self {
        case .recommended: return "推荐"
        case .stars:       return "星标数"
        case .updated:     return "最近更新"
        case .name:        return "名称"
        }
    }
}

// MARK: - GitHub 统计缓存

struct GitHubStats: Codable, Sendable {
    var stars: Int?
    var forks: Int?
    var language: String?
    var updatedAt: Date?
    var savedAt: Date
}

enum StatsStore {
    private static var fileURL: URL? {
        guard let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first else {
            return nil
        }
        return directory.appendingPathComponent("github-stats.json")
    }

    static func load() -> [String: GitHubStats] {
        guard
            let fileURL,
            let data = try? Data(contentsOf: fileURL),
            let decoded = try? JSONCoding.decoder.decode([String: GitHubStats].self, from: data)
        else { return [:] }
        return decoded
    }

    static func save(_ items: [ResourceItem]) {
        guard let fileURL else { return }
        let now = Date()
        var payload: [String: GitHubStats] = [:]
        for item in items where item.stars != nil {
            payload[item.id] = GitHubStats(
                stars: item.stars,
                forks: item.forks,
                language: item.language,
                updatedAt: item.lastUpdated,
                savedAt: now
            )
        }
        guard let data = try? JSONCoding.encoder.encode(payload) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }
}

// MARK: - 目录

@MainActor
@Observable
final class CatalogStore {
    enum Phase: Equatable {
        case idle
        case loading
        case ready
        case failed(String)
    }

    private(set) var phase: Phase = .idle
    private(set) var items: [ResourceItem] = []
    private(set) var news: [NewsItem] = []
    private(set) var trending: [GitHubRepo] = []

    private(set) var isRefreshing = false
    private(set) var lastRemoteRefresh: Date?
    /// GitHub 相关提示（限流、离线等），nil 表示一切正常。
    private(set) var remoteNotice: String?
    /// 资讯相关提示。
    private(set) var newsNotice: String?

    // MARK: 生命周期

    func loadIfNeeded() async {
        guard items.isEmpty, phase != .loading else { return }
        phase = .loading

        do {
            let seeded = try SeedLoader.resources()
            let cached = StatsStore.load()
            let now = Date()

            items = seeded.map { item in
                var copy = item
                if let stats = cached[item.id], now.timeIntervalSince(stats.savedAt) < 60 * 60 * 24 {
                    copy.stars = stats.stars ?? item.stars
                    copy.forks = stats.forks ?? item.forks
                    copy.language = stats.language ?? item.language
                    copy.lastUpdated = stats.updatedAt ?? item.lastUpdated
                }
                return copy
            }

            news = (try? SeedLoader.news()) ?? []
            phase = .ready
        } catch {
            phase = .failed(error.localizedDescription)
        }
    }

    // MARK: 派生数据

    var featured: [ResourceItem] {
        items.filter(\.isFeatured)
    }

    /// 「最新收录」：内置库的人工策展顺序（未经联网核对，UI 需标注）。
    var curatedHighlights: [ResourceItem] {
        Array(items.prefix(10))
    }

    /// 「最近更新」：只展示有真实更新时间的条目，绝不猜测。
    var recentlyUpdated: [ResourceItem] {
        items
            .filter { $0.lastUpdated != nil }
            .sorted { ($0.lastUpdated ?? .distantPast) > ($1.lastUpdated ?? .distantPast) }
    }

    /// 「本周热门」：有 Star 数据时按 Star 排序，否则退回精选。
    var weeklyPopular: [ResourceItem] {
        let ranked = items
            .filter { $0.stars != nil }
            .sorted { ($0.stars ?? 0) > ($1.stars ?? 0) }
        return ranked.isEmpty ? Array(featured.prefix(6)) : Array(ranked.prefix(6))
    }

    var latestNews: [NewsItem] {
        Array(news.prefix(3))
    }

    var categoriesInUse: [ResourceCategory] {
        let used = Set(items.map(\.category))
        return ResourceCategory.allCases.filter { used.contains($0) }
    }

    func items(in category: ResourceCategory) -> [ResourceItem] {
        items.filter { $0.category == category }
    }

    func item(id: String) -> ResourceItem? {
        items.first { $0.id == id }
    }

    func count(in category: ResourceCategory) -> Int {
        items.lazy.filter { $0.category == category }.count
    }

    // MARK: 搜索

    func search(_ rawQuery: String, category: ResourceCategory? = nil) -> [ResourceItem] {
        let tokens = Self.tokenize(rawQuery)
        guard !tokens.isEmpty else { return [] }

        var pool = items
        if let category {
            pool = pool.filter { $0.category == category }
        }

        let matched = pool.filter { item in
            let haystack = item.searchText
            return tokens.allSatisfy { haystack.contains($0) }
        }

        return matched.sorted { lhs, rhs in
            let lhsName = lhs.name.lowercased()
            let rhsName = rhs.name.lowercased()
            let lhsHit = tokens.contains { lhsName.contains($0) }
            let rhsHit = tokens.contains { rhsName.contains($0) }
            if lhsHit != rhsHit { return lhsHit }
            return (lhs.stars ?? 0) > (rhs.stars ?? 0)
        }
    }

    func sorted(_ list: [ResourceItem], by sort: ResourceSort) -> [ResourceItem] {
        switch sort {
        case .recommended:
            return list.sorted { lhs, rhs in
                if lhs.isFeatured != rhs.isFeatured { return lhs.isFeatured }
                return lhs.name.localizedStandardCompare(rhs.name) == .orderedAscending
            }
        case .stars:
            return list.sorted { ($0.stars ?? 0) > ($1.stars ?? 0) }
        case .updated:
            return list.sorted { ($0.lastUpdated ?? .distantPast) > ($1.lastUpdated ?? .distantPast) }
        case .name:
            return list.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        }
    }

    private static func tokenize(_ raw: String) -> [String] {
        raw.lowercased()
            .split(whereSeparator: { $0 == " " || $0 == "," || $0 == "，" || $0 == "、" || $0 == "\t" })
            .map(String.init)
    }

    // MARK: 联网刷新

    /// 下拉刷新 / 首页按钮触发。三类数据分别容错，谁失败都不影响其它。
    func refreshFromNetwork() async {
        guard !isRefreshing else { return }
        isRefreshing = true
        defer {
            isRefreshing = false
            lastRemoteRefresh = Date()
            UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: AppSettings.lastRemoteRefreshKey)
        }

        await refreshTrending()
        await refreshStats()
        await refreshNews()
    }

    private var api: GitHubAPI {
        var api = GitHubAPI()
        let token = AppSettings.githubToken
        api.token = token.isEmpty ? nil : token
        return api
    }

    private func refreshTrending() async {
        do {
            let repos = try await api.trendingRepositories(limit: 12)
            trending = repos
            remoteNotice = nil
        } catch let error as APIError {
            remoteNotice = error.userMessage
        } catch {
            remoteNotice = "GitHub 数据获取失败，请稍后重试。"
        }
    }

    /// 只为内置条目补齐 Star / 最近推送时间。
    /// 未认证配额只有 60 次/小时，因此限制单次刷新条数。
    private func refreshStats() async {
        var updated = items
        let limit = 15
        var requestCount = 0

        for index in updated.indices {
            if requestCount >= limit { break }
            guard
                let url = updated[index].githubURL,
                let slug = GitHubAPI.slug(from: url)
            else { continue }

            requestCount += 1
            do {
                let repo = try await api.repository(owner: slug.owner, name: slug.name)
                updated[index].stars = repo.stargazersCount
                updated[index].forks = repo.forksCount
                updated[index].language = repo.language
                if let pushed = repo.pushedAt ?? repo.updatedAt {
                    updated[index].lastUpdated = pushed
                }
            } catch let error as APIError {
                if case .rateLimited = error {
                    remoteNotice = error.userMessage
                    break
                }
                continue
            } catch {
                continue
            }
        }

        items = updated
        StatsStore.save(updated)
    }

    private func refreshNews() async {
        let result = await NewsLoader.loadAll(
            feeds: [.appleDeveloper, .githubBlog],
            limitPerFeed: 6
        )

        if !result.items.isEmpty {
            news = result.items
            newsNotice = result.failures.isEmpty
                ? nil
                : "部分信息源暂不可用：\(result.failures.joined(separator: "、"))"
        } else {
            newsNotice = "资讯源暂时无法访问（需要网络）。内置条目保持原样，不生成替代内容。"
        }
    }

    /// Developer / 安全公告等长列表页使用。
    func loadNews(feeds: [NewsFeed]) async {
        let result = await NewsLoader.loadAll(feeds: feeds, limitPerFeed: 10)
        if !result.items.isEmpty {
            news = result.items
        }
        newsNotice = result.items.isEmpty
            ? "资讯源暂时无法访问（需要网络）。"
            : (result.failures.isEmpty ? nil : "部分信息源暂不可用：\(result.failures.joined(separator: "、"))")
    }
}
