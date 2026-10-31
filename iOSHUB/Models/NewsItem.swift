//
//  NewsItem.swift
//  iOSHUB
//
//  资讯模型 + 可信信息源清单。
//  只聚合官方 / 社区原始链接，绝不生成或改写新闻内容。
//

import Foundation

struct NewsItem: Identifiable, Codable, Hashable, Sendable {
    let id: String
    var title: String
    var source: String
    var summary: String?
    var url: URL
    var publishedAt: Date?
    var feed: NewsFeed

    var publishedText: String {
        guard let publishedAt else { return "时间待验证" }
        return RelativeTime.string(from: publishedAt)
    }
}

enum NewsFeed: String, Codable, CaseIterable, Identifiable, Sendable {
    case appleDeveloper
    case appleNewsroom
    case appleSecurity
    case githubBlog
    case redditJailbreak

    var id: String { rawValue }

    var title: String {
        switch self {
        case .appleDeveloper: return "Apple Developer"
        case .appleNewsroom:  return "Apple Newsroom"
        case .appleSecurity:  return "Apple 安全更新"
        case .githubBlog:     return "GitHub Blog"
        case .redditJailbreak: return "r/jailbreak"
        }
    }

    /// 官方 RSS / 页面地址。解析失败时降级为「打开原文」。
    var feedURL: URL? {
        switch self {
        case .appleDeveloper:
            return URL(string: "https://developer.apple.com/news/rss/news.rss")
        case .appleNewsroom:
            return URL(string: "https://www.apple.com/newsroom/rss-feed.rss")
        case .appleSecurity:
            return URL(string: "https://support.apple.com/en-us/100100")
        case .githubBlog:
            return URL(string: "https://github.blog/feed/")
        case .redditJailbreak:
            return URL(string: "https://www.reddit.com/r/jailbreak/.rss")
        }
    }

    var siteURL: URL? {
        switch self {
        case .appleDeveloper:  return URL(string: "https://developer.apple.com/news/")
        case .appleNewsroom:   return URL(string: "https://www.apple.com/newsroom/")
        case .appleSecurity:   return URL(string: "https://support.apple.com/en-us/100100")
        case .githubBlog:      return URL(string: "https://github.blog/")
        case .redditJailbreak: return URL(string: "https://www.reddit.com/r/jailbreak/")
        }
    }

    var note: String {
        switch self {
        case .appleDeveloper:  return "苹果官方开发者公告，版本与 API 变更的第一手来源。"
        case .appleNewsroom:   return "苹果官方新闻稿，系统发布节奏与功能说明。"
        case .appleSecurity:   return "苹果官方安全更新列表，含 CVE 编号与受影响版本。"
        case .githubBlog:      return "GitHub 官方博客，含 Releases、Actions 等平台变更。"
        case .redditJailbreak: return "社区讨论聚合，内容未经审核，请以官方说明为准。"
        }
    }

    var symbol: String {
        switch self {
        case .appleDeveloper:  return "swift"
        case .appleNewsroom:   return "apple.logo"
        case .appleSecurity:   return "lock.shield.fill"
        case .githubBlog:      return "chevron.left.forwardslash.chevron.right"
        case .redditJailbreak: return "bubble.left.and.bubble.right.fill"
        }
    }
}
