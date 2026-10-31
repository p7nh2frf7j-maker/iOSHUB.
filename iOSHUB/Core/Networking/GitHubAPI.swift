//
//  GitHubAPI.swift
//  iOSHUB
//
//  GitHub REST API v3 客户端。
//  仅使用公开读接口；未认证时每小时 60 次，填写 Token 后 5000 次。
//  文档：https://docs.github.com/rest
//

import Foundation

// MARK: - 模型

struct GitHubRepo: Decodable, Identifiable, Hashable, Sendable {
    struct Owner: Decodable, Hashable, Sendable {
        let login: String
        let avatarURLString: String?

        enum CodingKeys: String, CodingKey {
            case login
            case avatarURLString = "avatar_url"
        }
    }

    let id: Int
    let name: String
    let fullName: String
    let owner: Owner
    let description: String?
    let htmlURL: URL
    let stargazersCount: Int
    let forksCount: Int
    let openIssuesCount: Int?
    let language: String?
    let topics: [String]?
    let pushedAt: Date?
    let updatedAt: Date?

    enum CodingKeys: String, CodingKey {
        case id, name, owner, description, language, topics
        case fullName = "full_name"
        case htmlURL = "html_url"
        case stargazersCount = "stargazers_count"
        case forksCount = "forks_count"
        case openIssuesCount = "open_issues_count"
        case pushedAt = "pushed_at"
        case updatedAt = "updated_at"
    }

    var avatarURL: URL? {
        owner.avatarURLString.flatMap { URL(string: $0) }
    }

    var updatedText: String {
        guard let updatedAt else { return "待验证" }
        return RelativeTime.string(from: updatedAt)
    }
}

struct GitHubRelease: Decodable, Sendable {
    let tagName: String
    let name: String?
    let body: String?
    let publishedAt: Date?
    let htmlURL: URL

    enum CodingKeys: String, CodingKey {
        case name, body
        case tagName = "tag_name"
        case publishedAt = "published_at"
        case htmlURL = "html_url"
    }
}

struct GitHubSearchResponse: Decodable, Sendable {
    let totalCount: Int
    let items: [GitHubRepo]

    enum CodingKeys: String, CodingKey {
        case totalCount = "total_count"
        case items
    }
}

enum GitHubSort: String, Sendable {
    case stars
    case forks
    case updated
}

// MARK: - 客户端

struct GitHubAPI: Sendable {
    /// 可选个人访问令牌（只读公开仓库权限即可）。
    var token: String?
    var client = HTTPClient()

    /// 经过 LinkRouter 前缀（自动 / 直连 / 镜像），换线路无需改任何调用点。
    private static var searchEndpoint: URL {
        URL(string: LinkRouter.apiPrefix + "https://api.github.com/search/repositories")!
    }
    private static var reposEndpoint: URL {
        URL(string: LinkRouter.apiPrefix + "https://api.github.com/repos")!
    }
    private static let trendingQuery = "topic:ios stars:>800 pushed:>{SINCE}"

    private var headers: [String: String] {
        var headers = [
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28"
        ]
        if let token, !token.isEmpty {
            headers["Authorization"] = "Bearer \(token)"
        }
        return headers
    }

    // MARK: 搜索

    func searchRepositories(
        query: String,
        sort: GitHubSort = .stars,
        order: String = "desc",
        page: Int = 1,
        perPage: Int = 20
    ) async throws -> GitHubSearchResponse {
        let data = try await client.get(
            Self.searchEndpoint,
            query: [
                URLQueryItem(name: "q", value: query),
                URLQueryItem(name: "sort", value: sort.rawValue),
                URLQueryItem(name: "order", value: order),
                URLQueryItem(name: "page", value: String(page)),
                URLQueryItem(name: "per_page", value: String(perPage))
            ],
            headers: headers
        )
        return try client.decode(GitHubSearchResponse.self, from: data)
    }

    /// 首页「GitHub 热门 iOS 项目」：近一年有推送、Star 800 以上。
    func trendingRepositories(limit: Int = 12) async throws -> [GitHubRepo] {
        let since = Self.dayString(from: Calendar.current.date(byAdding: .month, value: -12, to: Date()) ?? Date())
        let query = Self.trendingQuery.replacingOccurrences(of: "{SINCE}", with: since)
        let response = try await searchRepositories(query: query, sort: .stars, perPage: limit)
        return response.items
    }

    /// 按分类关键词检索。
    func searchRepositories(keyword: String, limit: Int = 20) async throws -> [GitHubRepo] {
        let response = try await searchRepositories(query: keyword, sort: .stars, perPage: limit)
        return response.items
    }

    // MARK: 单仓库

    func repository(owner: String, name: String) async throws -> GitHubRepo {
        let url = Self.reposEndpoint.appendingPathComponent(owner).appendingPathComponent(name)
        let data = try await client.get(url, headers: headers)
        return try client.decode(GitHubRepo.self, from: data)
    }

    func latestRelease(owner: String, name: String) async throws -> GitHubRelease? {
        let url = Self.reposEndpoint
            .appendingPathComponent(owner)
            .appendingPathComponent(name)
            .appendingPathComponent("releases/latest")
        do {
            let data = try await client.get(url, headers: headers)
            return try client.decode(GitHubRelease.self, from: data)
        } catch APIError.notFound {
            // 没有正式 Release 的仓库很常见，不视为错误。
            return nil
        }
    }

    func readme(owner: String, name: String) async throws -> String? {
        let url = Self.reposEndpoint
            .appendingPathComponent(owner)
            .appendingPathComponent(name)
            .appendingPathComponent("readme")
        let data = try await client.get(url, headers: headers)
        guard
            let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
            let content = object["content"] as? String
        else { return nil }
        let cleaned = content.replacingOccurrences(of: "\n", with: "")
        guard let decoded = Data(base64Encoded: cleaned) else { return nil }
        return String(data: decoded, encoding: .utf8)
    }

    // MARK: 工具

    /// 从 GitHub 页面地址解析 owner / repo。
    static func slug(from url: URL) -> (owner: String, name: String)? {
        let parts = url.pathComponents.filter { !$0.isEmpty && $0 != "/" }
        guard parts.count >= 2 else { return nil }
        return (parts[0], parts[1])
    }

    private static func dayString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
}

// MARK: - GitHub 仓库 -> 应用内资源模型

extension ResourceItem {
    static func make(from repo: GitHubRepo, category: ResourceCategory = .openSource) -> ResourceItem {
        ResourceItem(
            id: "github.\(repo.fullName.lowercased())",
            name: repo.name,
            category: category,
            summary: repo.description?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "该仓库暂无简介，请打开 GitHub 主页查看。",
            detail: nil,
            symbol: "chevron.left.forwardslash.chevron.right",
            iconURL: repo.avatarURL,
            version: nil,
            developer: repo.owner.login,
            developerURL: URL(string: "https://github.com/\(repo.owner.login)"),
            githubURL: repo.htmlURL,
            downloadURL: repo.htmlURL,
            sourceURL: repo.htmlURL,
            tutorialURL: nil,
            supportedIOS: [],
            supportedDevices: [],
            releaseDate: nil,
            lastUpdated: repo.pushedAt ?? repo.updatedAt,
            lastVerified: nil,
            compatibility: .pending,
            verificationNote: "该条目来自 GitHub 实时检索，兼容性需要在对应 Release 说明中确认。",
            risk: .community,
            tags: repo.topics ?? [],
            changelog: nil,
            screenshots: [],
            isFeatured: false,
            stars: repo.stargazersCount,
            forks: repo.forksCount,
            language: repo.language,
            latestRelease: nil,
            latestReleaseDate: nil
        )
    }
}
