//
//  ResourceDetailModel.swift
//  iOSHUB
//
//  详情页的远端增量数据：仓库实时统计、最新 Release、README。
//  与静态种子数据分离——种子给「这是什么」，这里给「现在怎么样了」。
//

import Foundation
import Observation

@MainActor
@Observable
final class ResourceDetailModel {
    enum State: Equatable {
        case idle
        case loading
        case loaded
        case failed(String)
    }

    private(set) var state: State = .idle

    // 仓库实时统计
    private(set) var stars: Int?
    private(set) var forks: Int?
    private(set) var language: String?
    private(set) var pushedAt: Date?

    // 最新 Release
    private(set) var release: GitHubRelease?
    private(set) var releaseNotes: [MarkdownBlock] = []

    // README
    private(set) var readmeBlocks: [MarkdownBlock] = []
    private(set) var readmeURL: URL?
    private(set) var readmeTruncated = false

    private let readmeCharacterLimit = 20_000

    var hasGitHubSource: Bool {
        readmeURL != nil || release != nil || stars != nil
    }

    func load(for item: ResourceItem) async {
        guard state == .idle else { return }

        guard
            let githubURL = item.githubURL,
            let slug = GitHubAPI.slug(from: githubURL)
        else {
            // 非 GitHub 资源：没有可增量拉取的内容，直接标记完成。
            state = .loaded
            return
        }

        state = .loading

        var api = GitHubAPI()
        let token = AppSettings.githubToken
        api.token = token.isEmpty ? nil : token

        // 1) 仓库统计（失败不影响其它）
        if let repo = try? await api.repository(owner: slug.owner, name: slug.name) {
            stars = repo.stargazersCount
            forks = repo.forksCount
            language = repo.language
            pushedAt = repo.pushedAt ?? repo.updatedAt
        }

        // 2) 最新 Release（很多项目没有正式 Release，属于正常情况）
        if let latest = try? await api.latestRelease(owner: slug.owner, name: slug.name) {
            release = latest
            if let body = latest.body, !body.isEmpty {
                releaseNotes = MarkdownParser.parse(String(body.prefix(4_000)), maxBlocks: 40)
            }
        }

        // 3) README
        do {
            if let markdown = try await api.readme(owner: slug.owner, name: slug.name) {
                readmeTruncated = markdown.count > readmeCharacterLimit
                readmeBlocks = MarkdownParser.parse(String(markdown.prefix(readmeCharacterLimit)))
                readmeURL = githubURL
            }
            state = .loaded
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("README 获取失败，请稍后重试。")
        }
    }

    /// 重试：清空状态后重新拉取。
    func reload(for item: ResourceItem) async {
        state = .idle
        readmeBlocks = []
        releaseNotes = []
        release = nil
        readmeURL = nil
        await load(for: item)
    }
}
